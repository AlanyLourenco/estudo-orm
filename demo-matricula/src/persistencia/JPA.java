package persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/** Cria a fábrica de EntityManager a partir do persistence.xml + dados de conexão. */
public class JPA {

    private static EntityManagerFactory fabrica;
    private static ConfiguracaoBanco configuracao;

    public static void iniciar(ConfiguracaoBanco cfg) throws SQLException {
        fechar();
        if (cfg.tipo == ConfiguracaoBanco.Tipo.POSTGRES) {
            criarBancoSeNaoExistir(cfg);
        }
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", cfg.url());
        props.put("jakarta.persistence.jdbc.user", cfg.tipo == ConfiguracaoBanco.Tipo.H2 ? "sa" : cfg.usuario);
        props.put("jakarta.persistence.jdbc.password", cfg.tipo == ConfiguracaoBanco.Tipo.H2 ? "" : cfg.senha);
        props.put(AvailableSettings.STATEMENT_INSPECTOR, MonitorSQL.INSTANCIA);
        fabrica = Persistence.createEntityManagerFactory("escola", props);
        configuracao = cfg;
    }

    public static EntityManager novoEntityManager() {
        return fabrica.createEntityManager();
    }

    public static ConfiguracaoBanco configuracao() {
        return configuracao;
    }

    public static void fechar() {
        if (fabrica != null && fabrica.isOpen()) {
            fabrica.close();
        }
        fabrica = null;
    }

    /** O banco escola_orm é criado na primeira execução; as tabelas, o Hibernate cria. */
    private static void criarBancoSeNaoExistir(ConfiguracaoBanco cfg) throws SQLException {
        try (Connection c = DriverManager.getConnection(cfg.urlServidor(), cfg.usuario, cfg.senha);
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, cfg.nomeBanco);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            if (!cfg.nomeBanco.matches("[a-z_][a-z0-9_]*")) {
                throw new SQLException("Nome de banco inválido: use letras minúsculas, números e _.");
            }
            try (Statement st = c.createStatement()) {
                st.execute("CREATE DATABASE " + cfg.nomeBanco);
            }
        }
    }
}
