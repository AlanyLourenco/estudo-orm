package visao;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

/** Cores e fontes da identidade visual da apresentação. */
public final class Tema {

    public static final Color TINTA = new Color(0x102C57);
    public static final Color TINTA_ESCURA = new Color(0x0A2147);
    public static final Color PAPEL = new Color(0xD7E0EB);
    public static final Color PAPEL_CLARO = new Color(0xE8EDF4);
    public static final Color LINHA = new Color(0xB3C1D3);
    public static final Color AZUL = new Color(0x2C62B8);
    public static final Color CEU = new Color(0xA9C4EA);
    public static final Color AMBAR = new Color(0xBC6A20);
    public static final Color AMBAR_CLARO = new Color(0xE09A55);
    public static final Color TEXTO_CLARO = new Color(0xEEF3F9);
    public static final Color TEXTO_MUDO = new Color(0x4F6485);
    public static final Color CREME = new Color(0xEDDFBE);

    public static final String SANS = "Segoe UI";
    public static final String MONO = "Consolas";

    private Tema() { }

    public static Font sans(int estilo, float tamanho) {
        return new Font(SANS, estilo, Math.round(tamanho));
    }

    public static Font mono(int estilo, float tamanho) {
        return new Font(MONO, estilo, Math.round(tamanho));
    }

    /** Rótulo pequeno em caixa alta, como nos slides. */
    public static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto.toUpperCase());
        l.setFont(sans(Font.BOLD, 12));
        l.setForeground(TEXTO_MUDO);
        return l;
    }

    public static JButton botao(String texto, Color fundo) {
        JButton b = new JButton(texto);
        b.setUI(new BasicButtonUI());
        b.setBackground(fundo);
        b.setForeground(Color.WHITE);
        b.setFont(sans(Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton botaoContorno(String texto) {
        JButton b = botao(texto, PAPEL_CLARO);
        b.setForeground(TINTA);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TINTA, 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        return b;
    }

    public static Border margem(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static Border cartao() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINHA),
                BorderFactory.createEmptyBorder(14, 18, 16, 18));
    }

    public static void semBorda(JComponent c) {
        c.setBorder(BorderFactory.createEmptyBorder());
    }

    public static Insets insets(int v, int h) {
        return new Insets(v, h, v, h);
    }
}
