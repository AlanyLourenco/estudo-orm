package visao;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import negocio.Escola;
import negocio.Escola.Item;
import negocio.Escola.LinhaAluno;
import negocio.Escola.Resultado;
import persistencia.JPA;
import persistencia.MonitorSQL;

/** Janela da demonstração: roteiro à esquerda, objetos no centro, SQL à direita. */
public class JanelaPrincipal extends JFrame {

    private final PainelSQL sql = new PainelSQL();
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[] { "id", "nome", "turma", "disciplinas" }, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);
    private final JTextArea resultado = new JTextArea();
    private final JTextArea codigo = new JTextArea();
    private final List<JButton> botoes = new ArrayList<>();

    // campos dos passos
    private final JTextField novoNome = new JTextField();
    private final JTextField novoEmail = new JTextField();
    private final JComboBox<Item> turma = new JComboBox<>();
    private final JPanel painelDisciplinas = new JPanel(new GridLayout(0, 2, 4, 2));
    private final List<JCheckBox> caixasDisciplina = new ArrayList<>();
    private final JSpinner idBuscar = spinner();
    private final JSpinner idRenomear = spinner();
    private final JSpinner idRemover = spinner();
    private final JSpinner idMatricular = spinner();
    private final JComboBox<Item> disciplinaMatricular = new JComboBox<>();
    private final JTextField nomeRenomear = new JTextField("Ana Beatriz");
    private float tamanhoTexto = 16f;

    public JanelaPrincipal() {
        super("ORM na prática · Matrícula escolar");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.PAPEL);
        setLayout(new BorderLayout());

        criarCaixasDisciplina();
        add(cabecalho(), BorderLayout.NORTH);
        add(roteiro(), BorderLayout.WEST);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centro(), direita());
        divisao.setResizeWeight(0.5);
        divisao.setBorder(BorderFactory.createEmptyBorder());
        divisao.setDividerSize(6);
        add(divisao, BorderLayout.CENTER);

        MonitorSQL.INSTANCIA.aoExecutar(sql::registrar);
        atalhosDeZoom();
        atualizarDadosDaTela();
        codigo.setText("// escolha um passo do roteiro à esquerda");
        resultado.setText("Dados iniciais carregados: 10 turmas, 4 disciplinas e 30 alunos.\n"
                + "As tabelas foram criadas pelo Hibernate a partir das classes.");

        setSize(1500, 900);
        setMinimumSize(new Dimension(1200, 720));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        javax.swing.SwingUtilities.invokeLater(() -> divisao.setDividerLocation(0.47));
    }

    // ------------------------------------------------------------------ montagem

    private JComponent cabecalho() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.TINTA);
        p.setBorder(Tema.margem(14, 24));
        JLabel titulo = new JLabel("<html>ORM na prática <span style='font-weight:normal;color:#A9C4EA'>· Matrícula escolar</span></html>");
        titulo.setFont(Tema.sans(Font.BOLD, 24));
        titulo.setForeground(Tema.TEXTO_CLARO);
        p.add(titulo, BorderLayout.WEST);
        JLabel info = new JLabel("<html><div style='text-align:right'>Hibernate 7 · Jakarta Persistence<br>"
                + JPA.configuracao().descricao() + "</div></html>");
        info.setFont(Tema.sans(Font.PLAIN, 13));
        info.setForeground(Tema.CEU);
        p.add(info, BorderLayout.EAST);
        return p;
    }

    private JComponent roteiro() {
        JPanel raiz = new PainelLarguraFixa(new BorderLayout());
        raiz.setBackground(Tema.PAPEL_CLARO);

        // --- detalhes de cada passo: os campos ficam junto do botão que os usa
        CardLayout cartas = new CardLayout();
        JPanel detalhes = new JPanel(cartas);
        detalhes.setOpaque(false);

        novoNome.setText("Ana Souza");
        detalhes.add(detalhe("Cadastrar aluno em disciplinas",
                "Cria o aluno com em.persist(). Se ele já existe (mesmo nome), só acrescenta as disciplinas.",
                "Observe: aluno novo → INSERT em aluno. Disciplina nova → INSERT em aluno_disciplina.",
                Tema.botao("Salvar", Tema.TINTA), this::criarAluno,
                lado(campo("Nome", novoNome), campo("Turma", turma)), campo("Disciplinas", painelDisciplinas)), "1");

        detalhes.add(detalhe("Buscar um aluno pelo id",
                "Chama em.find(Aluno.class, id): a linha do banco vira um objeto.",
                "Observe: um único SELECT, já com JOIN na tabela turma.",
                Tema.botao("Buscar pelo id", Tema.TINTA),
                () -> executar("2 · Buscar aluno pelo id", Escola.CODIGO_BUSCAR,
                        () -> Escola.buscarAluno(id(idBuscar)),
                        n -> "→ " + n + " SELECT: uma linha virou um objeto (a turma veio junto, com JOIN)."),
                campo("id do aluno", idBuscar), dicaTabela()), "2");

        detalhes.add(detalhe("Trocar o nome sem salvar",
                "Busca o aluno, muda o nome com setNome() e só faz commit. Nenhum save().",
                "Observe: o UPDATE aparece sozinho (dirty checking).",
                Tema.botao("Trocar o nome", Tema.TINTA),
                () -> executar("3 · Trocar o nome sem salvar", Escola.CODIGO_RENOMEAR,
                        () -> Escola.renomearAluno(id(idRenomear), nomeRenomear.getText().trim()),
                        n -> "→ o UPDATE apareceu sem nenhum save(): dirty checking."),
                campo("id do aluno", idRenomear), campo("Novo nome", nomeRenomear), dicaTabela()), "3");

        detalhes.add(detalhe("Listar turmas e seus alunos",
                "Busca as 10 turmas e, num laço, pede os alunos de cada uma.",
                "Observe o placar: 11 consultas (1 + 1 por turma). É o problema N+1.",
                Tema.botao("Listar turmas e alunos", Tema.AMBAR),
                () -> executar("4 · Listar turmas (laço com LAZY)", Escola.CODIGO_N_MAIS_1,
                        Escola::listarTurmasNmais1,
                        n -> "→ " + n + " consultas: 1 para as turmas + 1 para os alunos de cada turma (N+1)."),
                semCampos()), "4");

        detalhes.add(detalhe("Listar com JOIN FETCH",
                "A mesma listagem do passo 4, mas pedindo turmas e alunos numa consulta só.",
                "Observe o placar: 1 consulta, com o mesmo resultado.",
                Tema.botao("Listar com JOIN FETCH", Tema.AZUL),
                () -> executar("5 · Listar turmas com JOIN FETCH", Escola.CODIGO_JOIN_FETCH,
                        Escola::listarTurmasJoinFetch,
                        n -> "→ " + n + " consulta: o mesmo resultado, trazido de uma vez."),
                semCampos()), "5");

        detalhes.add(detalhe("Remover um aluno pelo id",
                "Busca o aluno e chama em.remove().",
                "Observe: o ORM apaga primeiro em aluno_disciplina, depois em aluno.",
                Tema.botao("Remover pelo id", Tema.TINTA),
                () -> executar("Extra · Remover aluno pelo id", Escola.CODIGO_REMOVER,
                        () -> Escola.removerAluno(id(idRemover)),
                        n -> "→ primeiro some da tabela de junção, depois da tabela aluno."),
                campo("id do aluno", idRemover), dicaTabela()), "remover");

        detalhes.add(detalhe("Matricular em mais uma disciplina",
                "Para um aluno que já existe: busca o aluno e adiciona a disciplina na coleção dele.",
                "Observe: nenhum save(); o INSERT em aluno_disciplina aparece no commit.",
                Tema.botao("Matricular", Tema.TINTA),
                () -> executar("Extra · Matricular em disciplina", Escola.CODIGO_MATRICULAR,
                        () -> Escola.matricular(id(idMatricular), ((Item) disciplinaMatricular.getSelectedItem()).id()),
                        n -> n > 3 ? "→ mexer na coleção virou INSERT na tabela de junção."
                                : "→ nenhum INSERT: a disciplina já estava na coleção."),
                campo("id do aluno", idMatricular), campo("Disciplina", disciplinaMatricular), dicaTabela()), "matricular");

        // --- lista de passos (escolher um mostra os campos dele embaixo)
        // { chave, número exibido, título, legenda }
        String[][] itens = {
            { "1", "1", "Cadastrar aluno", "INSERT" },
            { "2", "2", "Buscar pelo id", "SELECT" },
            { "3", "3", "Trocar o nome sem salvar", "UPDATE" },
            { "4", "4", "Listar turmas", "N+1" },
            { "5", "5", "Listar com JOIN FETCH", "1 consulta" },
            { "matricular", "+", "Matricular em disciplina", "INSERT N:N" },
            { "remover", "+", "Remover pelo id", "DELETE" },
        };
        JPanel lista = new JPanel(new GridLayout(0, 1, 0, 3));
        lista.setOpaque(false);
        List<JButton> seletores = new ArrayList<>();
        for (String[] it : itens) {
            JButton b = Tema.botao("<html><table cellpadding=0 cellspacing=0><tr>"
                    + "<td width=26 style='font-size:15px'><b>" + it[1] + "</b></td>"
                    + "<td><b>" + it[2] + "</b> <span style='font-size:9px'>" + it[3].toUpperCase()
                    + "</span></td></tr></table></html>", Tema.PAPEL_CLARO);
            b.setHorizontalAlignment(JButton.LEFT);
            b.setFont(Tema.sans(Font.PLAIN, 13));
            b.setBorder(Tema.margem(3, 12));
            b.addActionListener(e -> {
                cartas.show(detalhes, it[0]);
                for (JButton s : seletores) {
                    boolean ativo = s == b;
                    s.setBackground(ativo ? Tema.TINTA : Tema.PAPEL_CLARO);
                    s.setForeground(ativo ? Color.WHITE : Tema.TINTA);
                }
            });
            seletores.add(b);
            lista.add(b);
        }
        seletores.get(0).doClick();

        JPanel topo = new JPanel(new BorderLayout(0, 6));
        topo.setOpaque(false);
        topo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.LINHA),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel titulo = Tema.rotulo("Roteiro · escolha um passo");
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        topo.add(titulo, BorderLayout.NORTH);
        topo.add(lista, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.LINHA),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JButton limpar = Tema.botaoContorno("Limpar SQL");
        limpar.addActionListener(e -> sql.limpar());
        JLabel zoom = new JLabel("Ctrl + / Ctrl − : tamanho do texto");
        zoom.setFont(Tema.sans(Font.PLAIN, 12));
        zoom.setForeground(Tema.TEXTO_MUDO);
        rodape.add(zoom, BorderLayout.WEST);
        rodape.add(limpar, BorderLayout.EAST);

        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(detalhes, BorderLayout.CENTER);
        raiz.add(rodape, BorderLayout.SOUTH);

        JScrollPane rolagem = new JScrollPane(raiz);
        rolagem.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.LINHA));
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        rolagem.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setPreferredSize(new Dimension(350, 10));
        return rolagem;
    }

    /** Painel de um passo: o que faz, os campos que usa, o botão e o que observar. */
    private JComponent detalhe(String titulo, String descricao, String observar,
            JButton botao, Runnable acao, JComponent... campos) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel t = new JLabel(titulo);
        t.setFont(Tema.sans(Font.BOLD, 17));
        t.setForeground(Tema.TINTA);
        p.add(alinhar(t));
        p.add(Box.createVerticalStrut(4));
        p.add(alinhar(texto(descricao, Tema.TEXTO_MUDO)));
        p.add(Box.createVerticalStrut(10));

        for (JComponent c : campos) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            p.add(alinhar(c));
            p.add(Box.createVerticalStrut(6));
        }

        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        botao.setFont(Tema.sans(Font.BOLD, 15));
        botao.addActionListener(e -> acao.run());
        botoes.add(botao);
        p.add(alinhar(botao));
        p.add(Box.createVerticalStrut(10));

        JLabel obs = texto(observar, Tema.TINTA);
        obs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Tema.AZUL),
                BorderFactory.createEmptyBorder(2, 10, 2, 0)));
        p.add(alinhar(obs));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JLabel texto(String s, Color cor) {
        JLabel l = new JLabel("<html><div style='width:230px'>" + s + "</div></html>");
        l.setFont(Tema.sans(Font.PLAIN, 13));
        l.setForeground(cor);
        return l;
    }

    private static JComponent lado(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        return p;
    }

    /** Painel que sempre ocupa a largura da coluna: só rola na vertical. */
    private static class PainelLarguraFixa extends JPanel implements javax.swing.Scrollable {
        PainelLarguraFixa(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private static JComponent dicaTabela() {
        return texto("Dica: clique numa linha da tabela para preencher o id.", Tema.TEXTO_MUDO);
    }

    private static JComponent semCampos() {
        return texto("Este passo não usa nenhum campo: é só clicar.", Tema.TEXTO_MUDO);
    }

    private JComponent centro() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(Tema.PAPEL);

        // mundo dos objetos
        JPanel objetos = new JPanel(new BorderLayout(0, 8));
        objetos.setOpaque(false);
        objetos.setBorder(Tema.margem(16, 20));
        objetos.add(titulo("Mundo dos objetos", "o que a aplicação enxerga: objetos Aluno com Turma e Disciplinas"),
                BorderLayout.NORTH);
        estilizarTabela();
        JScrollPane rolagemTabela = new JScrollPane(tabela);
        rolagemTabela.setBorder(BorderFactory.createLineBorder(Tema.LINHA));
        rolagemTabela.getViewport().setBackground(Color.WHITE);
        objetos.add(rolagemTabela, BorderLayout.CENTER);
        p.add(objetos, BorderLayout.CENTER);

        // resultado
        JPanel r = new JPanel(new BorderLayout(0, 8));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        r.setPreferredSize(new Dimension(10, 250));
        r.add(titulo("Resultado", "o que o código recebeu de volta"), BorderLayout.NORTH);
        resultado.setEditable(false);
        resultado.setLineWrap(true);
        resultado.setWrapStyleWord(true);
        resultado.setBackground(Tema.PAPEL_CLARO);
        resultado.setForeground(Tema.TINTA);
        resultado.setBorder(Tema.margem(12, 14));
        JScrollPane rr = new JScrollPane(resultado);
        rr.setBorder(BorderFactory.createLineBorder(Tema.LINHA));
        r.add(rr, BorderLayout.CENTER);
        p.add(r, BorderLayout.SOUTH);
        aplicarTamanhoTexto();
        return p;
    }

    /** Coluna da direita: o código Java do passo em cima, o SQL gerado embaixo. */
    private JComponent direita() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.TINTA_ESCURA);

        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(Tema.TINTA);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.TINTA_ESCURA),
                Tema.margem(12, 20)));
        JLabel t = new JLabel("Código Java executado");
        t.setFont(Tema.sans(Font.BOLD, 16));
        t.setForeground(Tema.CEU);
        c.add(t, BorderLayout.NORTH);
        codigo.setEditable(false);
        codigo.setBackground(Tema.TINTA);
        codigo.setForeground(Tema.TEXTO_CLARO);
        codigo.setBorder(BorderFactory.createEmptyBorder());
        codigo.setRows(7);
        c.add(codigo, BorderLayout.CENTER);

        p.add(c, BorderLayout.NORTH);
        p.add(sql, BorderLayout.CENTER);
        return p;
    }

    private void estilizarTabela() {
        tabela.setFont(Tema.sans(Font.PLAIN, 15));
        tabela.setRowHeight(28);
        tabela.setShowVerticalLines(false);
        tabela.setGridColor(new Color(0xE1E7EF));
        tabela.setSelectionBackground(Tema.CEU);
        tabela.setSelectionForeground(Tema.TINTA);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getTableHeader().setFont(Tema.sans(Font.BOLD, 13));
        tabela.getTableHeader().setBackground(Tema.TINTA);
        tabela.getTableHeader().setForeground(Color.WHITE);
        tabela.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer cabecalho = new DefaultTableCellRenderer();
        cabecalho.setBackground(Tema.TINTA);
        cabecalho.setForeground(Color.WHITE);
        cabecalho.setFont(Tema.sans(Font.BOLD, 13));
        cabecalho.setBorder(Tema.margem(6, 8));
        tabela.getTableHeader().setDefaultRenderer(cabecalho);
        int[] larguras = { 36, 150, 80, 260 };
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (!e.getValueIsAdjusting() && linha >= 0) {
                Object id = modeloTabela.getValueAt(linha, 0);
                for (JSpinner s : new JSpinner[] { idBuscar, idRenomear, idRemover, idMatricular }) {
                    s.setValue(((Long) id).intValue());
                }
            }
        });
    }

    private JComponent titulo(String titulo, String subtitulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.sans(Font.BOLD, 18));
        t.setForeground(Tema.TINTA);
        JLabel s = new JLabel(subtitulo);
        s.setFont(Tema.sans(Font.PLAIN, 13));
        s.setForeground(Tema.TEXTO_MUDO);
        p.add(t, BorderLayout.NORTH);
        p.add(s, BorderLayout.SOUTH);
        return p;
    }

    private JComponent campo(String rotulo, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(rotulo);
        l.setFont(Tema.sans(Font.PLAIN, 13));
        l.setForeground(Tema.TEXTO_MUDO);
        p.add(l, BorderLayout.NORTH);
        campo.setFont(Tema.sans(Font.PLAIN, 14));
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private static <T extends JComponent> T alinhar(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JSpinner spinner() {
        JSpinner s = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return s;
    }

    private static Long id(JSpinner s) {
        return ((Number) s.getValue()).longValue();
    }

    // ------------------------------------------------------------------ ações

    private void criarAluno() {
        String nome = novoNome.getText().trim();
        if (nome.isEmpty()) {
            resultado.setText("Digite um nome para o aluno.");
            return;
        }
        String email = novoEmail.getText().trim().isEmpty() ? Escola.email(nome) : novoEmail.getText().trim();
        Item t = (Item) turma.getSelectedItem();
        List<Long> disciplinas = new ArrayList<>();
        for (JCheckBox cb : caixasDisciplina) {
            if (cb.isSelected()) {
                disciplinas.add((Long) cb.getClientProperty("id"));
            }
        }
        executar("1 · Cadastrar " + nome, Escola.CODIGO_CRIAR,
                () -> Escola.cadastrarAluno(nome, email, t == null ? null : t.id(), disciplinas),
                n -> "→ aluno novo: INSERT em aluno + disciplinas; aluno que já existe: só os INSERTs das disciplinas novas.");
    }

    /** Roda um passo fora da thread da tela e mostra resultado, código e SQL. */
    private void executar(String passo, String trecho, Supplier<Resultado> acao, IntFunction<String> resumo) {
        botoes.forEach(b -> b.setEnabled(false));
        sql.iniciarPasso(passo);
        MonitorSQL.INSTANCIA.zerar();
        codigo.setText(trecho);
        codigo.setCaretPosition(0);
        resultado.setForeground(Tema.TINTA);
        resultado.setText("executando…");
        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() {
                return acao.get();
            }

            @Override
            protected void done() {
                Long idAluno = null;
                try {
                    Resultado r = get();
                    idAluno = r.idAluno();
                    resultado.setText(String.join("\n", r.linhas()));
                    resultado.setCaretPosition(0);
                    int total = MonitorSQL.INSTANCIA.total();
                    sql.anotar(resumo.apply(total));
                } catch (Exception e) {
                    resultado.setForeground(Tema.AMBAR);
                    resultado.setText("Erro: " + mensagem(e));
                    sql.anotar("→ a transação foi desfeita (rollback).");
                }
                atualizarDadosDaTela();
                selecionarAluno(idAluno);
                botoes.forEach(b -> b.setEnabled(true));
            }
        }.execute();
    }

    /** Destaca na tabela o aluno envolvido no passo (e preenche os campos de id). */
    private void selecionarAluno(Long id) {
        if (id == null) {
            return;
        }
        for (int linha = 0; linha < modeloTabela.getRowCount(); linha++) {
            if (id.equals(modeloTabela.getValueAt(linha, 0))) {
                tabela.setRowSelectionInterval(linha, linha);
                tabela.scrollRectToVisible(tabela.getCellRect(linha, 0, true));
                return;
            }
        }
    }

    private static String mensagem(Throwable e) {
        Throwable c = e;
        while (true) {
            // 23505 = violação de UNIQUE (código padrão do SQL, igual em qualquer idioma)
            if (c instanceof java.sql.SQLException s && "23505".equals(s.getSQLState())) {
                return "já existe um aluno com esse e-mail.\n\n"
                        + "A coluna email é UNIQUE (@Column(unique = true)): o banco recusou "
                        + "o segundo cadastro e o ORM desfez a transação.\n"
                        + "Para cadastrar, use outro nome (o e-mail é gerado a partir dele).";
            }
            if (c.getCause() == null || c.getCause() == c) {
                break;
            }
            c = c.getCause();
        }
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }

    private void atualizarDadosDaTela() {
        List<LinhaAluno> alunos = Escola.alunosParaTabela();
        modeloTabela.setRowCount(0);
        for (LinhaAluno a : alunos) {
            modeloTabela.addRow(new Object[] { a.id(), a.nome(), a.turma(), a.disciplinas() });
        }

        Item selecionada = (Item) turma.getSelectedItem();
        turma.removeAllItems();
        for (Item t : Escola.turmas()) {
            turma.addItem(t);
            if (selecionada != null && selecionada.id().equals(t.id())) {
                turma.setSelectedItem(t);
            }
        }

    }

    /** As disciplinas não mudam durante a demonstração: criadas uma vez só. */
    private void criarCaixasDisciplina() {
        painelDisciplinas.setOpaque(false);
        boolean primeira = true;
        for (Item d : Escola.disciplinas()) {
            JCheckBox cb = new JCheckBox(d.nome(), primeira);
            primeira = false;
            cb.putClientProperty("id", d.id());
            cb.setOpaque(false);
            cb.setFont(Tema.sans(Font.PLAIN, 13));
            caixasDisciplina.add(cb);
            painelDisciplinas.add(cb);
            disciplinaMatricular.addItem(d);
        }
    }

    private void atalhosDeZoom() {
        JComponent raiz = getRootPane();
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "maior");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "maior");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "menor");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "menor");
        raiz.getActionMap().put("maior", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoom(2f);
            }
        });
        raiz.getActionMap().put("menor", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoom(-2f);
            }
        });
    }

    private void zoom(float delta) {
        sql.alterarTamanho(delta);
        tamanhoTexto = Math.max(11f, Math.min(30f, tamanhoTexto + delta));
        aplicarTamanhoTexto();
        tabela.setFont(Tema.sans(Font.PLAIN, tamanhoTexto - 1));
        tabela.setRowHeight(Math.round(tamanhoTexto * 1.8f));
    }

    private void aplicarTamanhoTexto() {
        resultado.setFont(Tema.sans(Font.PLAIN, tamanhoTexto));
        codigo.setFont(Tema.mono(Font.PLAIN, tamanhoTexto - 1));
    }
}
