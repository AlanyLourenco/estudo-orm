package visao;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Painel escuro que mostra, em tempo real, cada comando SQL enviado ao banco. */
public class PainelSQL extends JPanel {

    private static final Pattern PALAVRAS = Pattern.compile(
            "\\b(select|from|where|join|left|inner|on|insert|into|values|update|set|delete|order by|distinct|and|or|as|limit|fetch|first|rows|only)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUEBRAS = Pattern.compile(
            "\\s+(?=(from|left join|where|order by|values|set)\\b)|(?<!left)\\s+(?=join\\b)", Pattern.CASE_INSENSITIVE);

    private final JTextPane texto = new JTextPane();
    private final JLabel contador = new JLabel("0");
    private final JLabel legenda = new JLabel("comandos neste passo");
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
    private final SimpleAttributeSet palavra = new SimpleAttributeSet();
    private final SimpleAttributeSet numero = new SimpleAttributeSet();
    private final SimpleAttributeSet titulo = new SimpleAttributeSet();
    private final SimpleAttributeSet nota = new SimpleAttributeSet();
    private int noPasso;
    private float tamanho = 16f;

    public PainelSQL() {
        super(new BorderLayout());
        setBackground(Tema.TINTA_ESCURA);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(Tema.TINTA);
        topo.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 28));
        JPanel textos = new JPanel(new BorderLayout());
        textos.setOpaque(false);
        JLabel t = new JLabel("SQL enviado ao banco");
        t.setFont(Tema.sans(Font.BOLD, 20));
        t.setForeground(Tema.TEXTO_CLARO);
        JLabel sub = new JLabel("gerado pelo Hibernate");
        sub.setFont(Tema.sans(Font.PLAIN, 13));
        sub.setForeground(Tema.CEU);
        textos.add(t, BorderLayout.NORTH);
        textos.add(sub, BorderLayout.SOUTH);
        topo.add(textos, BorderLayout.CENTER);

        JPanel placar = new JPanel(new BorderLayout());
        placar.setOpaque(false);
        contador.setFont(Tema.sans(Font.BOLD, 40));
        contador.setForeground(Tema.CEU);
        contador.setHorizontalAlignment(JLabel.RIGHT);
        legenda.setFont(Tema.sans(Font.PLAIN, 12));
        legenda.setForeground(Tema.CEU);
        placar.add(contador, BorderLayout.CENTER);
        placar.add(legenda, BorderLayout.SOUTH);
        topo.add(placar, BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        texto.setEditable(false);
        texto.setBackground(Tema.TINTA_ESCURA);
        texto.setBorder(Tema.margem(14, 20));
        JScrollPane rolagem = new JScrollPane(texto);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        add(rolagem, BorderLayout.CENTER);
        aplicarEstilos();
    }

    private void aplicarEstilos() {
        estilo(normal, new Color(0xE3EAF3), false, false);
        estilo(palavra, new Color(0x9FC1F2), true, false);
        estilo(numero, Tema.AMBAR_CLARO, true, false);
        estilo(titulo, Tema.CREME, true, false);
        estilo(nota, new Color(0x8EA3C2), false, true);
    }

    private void estilo(SimpleAttributeSet s, Color cor, boolean negrito, boolean italico) {
        StyleConstants.setFontFamily(s, Tema.MONO);
        StyleConstants.setFontSize(s, Math.round(tamanho));
        StyleConstants.setForeground(s, cor);
        StyleConstants.setBold(s, negrito);
        StyleConstants.setItalic(s, italico);
    }

    public void alterarTamanho(float delta) {
        tamanho = Math.max(11f, Math.min(30f, tamanho + delta));
        aplicarEstilos();
        StyledDocument doc = texto.getStyledDocument();
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontSize(s, Math.round(tamanho));
        doc.setCharacterAttributes(0, doc.getLength(), s, false);
    }

    /** Abre um novo bloco para o passo que vai começar. */
    public void iniciarPasso(String nome) {
        noPasso = 0;
        contador.setText("0");
        contador.setForeground(Tema.CEU);
        StyledDocument doc = texto.getStyledDocument();
        if (doc.getLength() > 0) {
            escrever("\n", normal);
        }
        escrever("▶ " + nome + "\n", titulo);
    }

    /** Chamado pelo MonitorSQL (em qualquer thread). */
    public void registrar(String sql) {
        SwingUtilities.invokeLater(() -> {
            noPasso++;
            contador.setText(String.valueOf(noPasso));
            contador.setForeground(noPasso > 3 ? Tema.AMBAR_CLARO : Tema.CEU);
            escrever(String.format("%3d  ", noPasso), numero);
            String legivel = sql.trim().replaceAll(",(?=\\S)", ", ");
            escreverSQL(QUEBRAS.matcher(legivel).replaceAll("\n     "));
            escrever("\n", normal);
        });
    }

    public void anotar(String texto) {
        SwingUtilities.invokeLater(() -> escrever("     " + texto + "\n", nota));
    }

    public void limpar() {
        texto.setText("");
        noPasso = 0;
        contador.setText("0");
        contador.setForeground(Tema.CEU);
    }

    private void escreverSQL(String sql) {
        Matcher m = PALAVRAS.matcher(sql);
        int ultimo = 0;
        while (m.find()) {
            escrever(sql.substring(ultimo, m.start()), normal);
            escrever(m.group().toUpperCase(), palavra);
            ultimo = m.end();
        }
        escrever(sql.substring(ultimo), normal);
    }

    private void escrever(String s, SimpleAttributeSet estilo) {
        StyledDocument doc = texto.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), s, estilo);
        } catch (BadLocationException e) {
            // não acontece: sempre inserimos no fim
        }
        texto.setCaretPosition(doc.getLength());
    }
}
