package visao;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import negocio.Escola;
import persistencia.ConfiguracaoBanco;
import persistencia.JPA;

/**
 * Tela de conexão com o PostgreSQL. Só aparece quando não há senha salva
 * em db.properties ou quando a conexão automática falhou.
 */
public class DialogoConexao extends JDialog {

    private final ConfiguracaoBanco cfg;
    private final JTextField host = new JTextField(16);
    private final JTextField porta = new JTextField(6);
    private final JTextField banco = new JTextField(16);
    private final JTextField usuario = new JTextField(16);
    private final JPasswordField senha = new JPasswordField(16);
    private final JCheckBox lembrar = new JCheckBox("lembrar a senha (não perguntar de novo)", true);
    private final JLabel status = new JLabel(" ");
    private boolean conectado;

    public DialogoConexao(ConfiguracaoBanco cfg, String erroAnterior) {
        super((java.awt.Frame) null, "ORM na prática · conexão com o PostgreSQL", true);
        this.cfg = cfg;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.PAPEL);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(Tema.TINTA);
        topo.setBorder(Tema.margem(18, 24));
        JLabel titulo = new JLabel("Conexão com o PostgreSQL");
        titulo.setFont(Tema.sans(Font.BOLD, 20));
        titulo.setForeground(Tema.TEXTO_CLARO);
        JLabel sub = new JLabel("Matrícula escolar · demonstração de ORM");
        sub.setFont(Tema.sans(Font.PLAIN, 13));
        sub.setForeground(Tema.CEU);
        topo.add(titulo, BorderLayout.NORTH);
        topo.add(sub, BorderLayout.SOUTH);
        raiz.add(topo, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(Tema.margem(18, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        linha(form, g, "Servidor", host);
        linha(form, g, "Porta", porta);
        linha(form, g, "Banco (criado se não existir)", banco);
        linha(form, g, "Usuário", usuario);
        linha(form, g, "Senha", senha);
        g.gridx = 1;
        lembrar.setOpaque(false);
        form.add(lembrar, g);
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        status.setFont(Tema.sans(Font.PLAIN, 13));
        form.add(status, g);
        raiz.add(form, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.setBorder(BorderFactory.createEmptyBorder(0, 24, 18, 24));
        JButton sair = Tema.botaoContorno("Sair");
        JButton conectar = Tema.botao("Conectar", Tema.TINTA);
        sair.addActionListener(e -> dispose());
        conectar.addActionListener(e -> conectar(conectar));
        botoes.add(sair, BorderLayout.WEST);
        botoes.add(conectar, BorderLayout.EAST);
        raiz.add(botoes, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(conectar);

        host.setText(cfg.host);
        porta.setText(String.valueOf(cfg.porta));
        banco.setText(cfg.nomeBanco);
        usuario.setText(cfg.usuario);
        senha.setText(cfg.senha);
        if (erroAnterior != null) {
            mostrarErro(erroAnterior);
        }

        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    private void linha(JPanel form, GridBagConstraints g, String nome, JComponent campo) {
        JLabel l = new JLabel(nome);
        l.setFont(Tema.sans(Font.PLAIN, 14));
        g.gridx = 0;
        g.insets = new Insets(4, 0, 4, 16);
        form.add(l, g);
        g.gridx = 1;
        campo.setFont(Tema.sans(Font.PLAIN, 14));
        form.add(campo, g);
        g.gridy++;
    }

    private void mostrarErro(String mensagem) {
        String msg = String.valueOf(mensagem).replace("&", "&amp;").replace("<", "&lt;");
        status.setForeground(Tema.AMBAR);
        status.setText("<html><div style='width:360px'>Não foi possível conectar:<br>" + msg + "</div></html>");
    }

    private void conectar(JButton botao) {
        cfg.tipo = ConfiguracaoBanco.Tipo.POSTGRES;
        cfg.host = host.getText().trim();
        cfg.nomeBanco = banco.getText().trim();
        cfg.usuario = usuario.getText().trim();
        cfg.senha = new String(senha.getPassword());
        try {
            cfg.porta = Integer.parseInt(porta.getText().trim());
        } catch (NumberFormatException e) {
            mostrarErro("porta inválida.");
            return;
        }
        botao.setEnabled(false);
        status.setForeground(Tema.TEXTO_MUDO);
        status.setText("Conectando e recriando as tabelas…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                JPA.iniciar(cfg);
                Escola.popularDados();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cfg.salvar(lembrar.isSelected());
                    conectado = true;
                    dispose();
                } catch (Exception e) {
                    mostrarErro(causaRaiz(e));
                    botao.setEnabled(true);
                    pack();
                }
            }
        }.execute();
    }

    public boolean conectado() {
        return conectado;
    }

    public static String causaRaiz(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
