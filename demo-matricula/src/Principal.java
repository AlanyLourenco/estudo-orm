import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import negocio.Escola;
import persistencia.ConfiguracaoBanco;
import persistencia.JPA;
import persistencia.MonitorSQL;
import visao.DialogoConexao;
import visao.JanelaPrincipal;
import visao.Tema;

/**
 * Demonstração de ORM (Hibernate) para o seminário.
 * Uso normal: conecta no PostgreSQL com os dados de db.properties e abre a janela
 * (a tela de conexão só aparece se não houver senha salva ou se a conexão falhar).
 * Com "--teste": roda o roteiro no console, em H2 (sem PostgreSQL).
 */
public class Principal {

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);

        if (args.length > 0 && args[0].equals("--teste")) {
            testarNoConsole();
            return;
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        ConfiguracaoBanco cfg = ConfiguracaoBanco.carregar();
        cfg.tipo = ConfiguracaoBanco.Tipo.POSTGRES;

        if (cfg.senha.isEmpty()) {
            SwingUtilities.invokeLater(() -> pedirConexao(cfg, null));
            return;
        }

        // senha salva em db.properties: conecta direto, sem a tela de conexão
        JWindow aviso = avisoConectando(cfg);
        SwingUtilities.invokeLater(() -> aviso.setVisible(true));
        String erro = null;
        try {
            JPA.iniciar(cfg);
            Escola.popularDados();
        } catch (Exception e) {
            erro = DialogoConexao.causaRaiz(e);
        }
        String erroFinal = erro;
        SwingUtilities.invokeLater(() -> {
            aviso.dispose();
            if (erroFinal == null) {
                new JanelaPrincipal().setVisible(true);
            } else {
                pedirConexao(cfg, erroFinal);
            }
        });
    }

    /** Tela de conexão: só quando não há senha salva ou quando a conexão falhou. */
    private static void pedirConexao(ConfiguracaoBanco cfg, String erro) {
        DialogoConexao dialogo = new DialogoConexao(cfg, erro);
        dialogo.setVisible(true);
        if (dialogo.conectado()) {
            new JanelaPrincipal().setVisible(true);
        } else {
            System.exit(0);
        }
    }

    private static JWindow avisoConectando(ConfiguracaoBanco cfg) {
        JWindow w = new JWindow();
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Tema.TINTA);
        p.setBorder(BorderFactory.createEmptyBorder(22, 30, 22, 30));
        JLabel t = new JLabel("Conectando ao PostgreSQL…");
        t.setFont(Tema.sans(Font.BOLD, 18));
        t.setForeground(Tema.TEXTO_CLARO);
        JLabel s = new JLabel(cfg.descricao() + " · recriando as tabelas");
        s.setFont(Tema.sans(Font.PLAIN, 13));
        s.setForeground(Tema.CEU);
        p.add(t, BorderLayout.NORTH);
        p.add(s, BorderLayout.SOUTH);
        w.setContentPane(p);
        w.pack();
        w.setLocationRelativeTo(null);
        return w;
    }

    /** Executa os 5 passos em H2 e mostra quantos comandos SQL cada um gerou. */
    private static void testarNoConsole() throws Exception {
        ConfiguracaoBanco cfg = new ConfiguracaoBanco();
        cfg.tipo = ConfiguracaoBanco.Tipo.H2;
        JPA.iniciar(cfg);
        Escola.popularDados();
        MonitorSQL.INSTANCIA.aoExecutar(sql -> System.out.println("      [SQL] " + sql));

        String ana = Escola.email("Ana Souza");
        passo("1 cadastrar Ana em Banco de Dados", () -> Escola.cadastrarAluno("Ana Souza", ana, 1L, List.of(1L)));
        passo("1 cadastrar a mesma Ana em POO", () -> Escola.cadastrarAluno("Ana Souza", ana, 1L, List.of(2L)));
        passo("1 cadastrar a Ana em POO de novo", () -> Escola.cadastrarAluno("Ana Souza", ana, 2L, List.of(2L)));
        passo("2 buscar", () -> Escola.buscarAluno(31L));
        passo("3 renomear", () -> Escola.renomearAluno(31L, "Ana Beatriz"));
        passo("+ matricular em Estruturas", () -> Escola.matricular(31L, 3L));
        passo("+ matricular de novo (repetida)", () -> Escola.matricular(31L, 3L));
        passo("4 N+1", Escola::listarTurmasNmais1);
        passo("5 JOIN FETCH", Escola::listarTurmasJoinFetch);
        passo("+ remover", () -> Escola.removerAluno(31L));
        System.out.println("alunos na tabela: " + Escola.alunosParaTabela().size());
        JPA.fechar();
    }

    private static void passo(String nome, java.util.function.Supplier<Escola.Resultado> acao) {
        System.out.println("=== " + nome);
        MonitorSQL.INSTANCIA.zerar();
        Escola.Resultado r = acao.get();
        r.linhas().stream().limit(3).forEach(l -> System.out.println("   " + l));
        System.out.println("   >> comandos SQL: " + MonitorSQL.INSTANCIA.total());
    }
}
