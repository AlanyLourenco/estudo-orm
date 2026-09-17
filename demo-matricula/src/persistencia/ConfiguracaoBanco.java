package persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Dados de conexão lidos (e salvos) em db.properties, na pasta da demonstração. */
public class ConfiguracaoBanco {

    public enum Tipo { POSTGRES, H2 }

    private static final Path ARQUIVO = Path.of("db.properties");

    public Tipo tipo = Tipo.POSTGRES;
    public String host = "localhost";
    public int porta = 5432;
    public String nomeBanco = "escola_orm";
    public String usuario = "postgres";
    public String senha = "";

    public static ConfiguracaoBanco carregar() {
        ConfiguracaoBanco c = new ConfiguracaoBanco();
        if (Files.exists(ARQUIVO)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(ARQUIVO)) {
                p.load(in);
            } catch (IOException e) {
                return c;
            }
            c.tipo = "h2".equalsIgnoreCase(p.getProperty("banco", "postgres")) ? Tipo.H2 : Tipo.POSTGRES;
            c.host = p.getProperty("host", c.host);
            c.porta = Integer.parseInt(p.getProperty("porta", String.valueOf(c.porta)));
            c.nomeBanco = p.getProperty("nome", c.nomeBanco);
            c.usuario = p.getProperty("usuario", c.usuario);
            c.senha = p.getProperty("senha", c.senha);
        }
        return c;
    }

    /** Salva tudo, exceto a senha quando lembrarSenha for falso. */
    public void salvar(boolean lembrarSenha) {
        Properties p = new Properties();
        p.setProperty("banco", tipo == Tipo.H2 ? "h2" : "postgres");
        p.setProperty("host", host);
        p.setProperty("porta", String.valueOf(porta));
        p.setProperty("nome", nomeBanco);
        p.setProperty("usuario", usuario);
        p.setProperty("senha", lembrarSenha ? senha : "");
        try (OutputStream out = Files.newOutputStream(ARQUIVO)) {
            p.store(out, "banco = postgres ou h2 (h2 roda em memoria, sem instalar nada)");
        } catch (IOException e) {
            // não salvar a configuração não impede a demonstração
        }
    }

    public String urlServidor() {
        return "jdbc:postgresql://" + host + ":" + porta + "/postgres";
    }

    public String url() {
        return tipo == Tipo.H2
                ? "jdbc:h2:mem:" + nomeBanco + ";DB_CLOSE_DELAY=-1"
                : "jdbc:postgresql://" + host + ":" + porta + "/" + nomeBanco;
    }

    public String descricao() {
        return tipo == Tipo.H2
                ? "H2 em memória"
                : "PostgreSQL · " + host + ":" + porta + "/" + nomeBanco;
    }
}
