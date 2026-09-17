package persistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Recebe cada comando SQL que o Hibernate está prestes a enviar ao banco.
 * É daqui que a janela tira o painel "SQL enviado ao banco" e a contagem de consultas.
 */
public class MonitorSQL implements StatementInspector {

    public static final MonitorSQL INSTANCIA = new MonitorSQL();

    private final List<Consumer<String>> ouvintes = new ArrayList<>();
    private volatile boolean pausado;
    private int contador;

    private MonitorSQL() { }

    @Override
    public String inspect(String sql) {
        if (!pausado) {
            synchronized (this) {
                contador++;
            }
            for (Consumer<String> o : ouvintes) {
                o.accept(sql);
            }
        }
        return sql; // o SQL segue para o banco sem alteração
    }

    public void aoExecutar(Consumer<String> ouvinte) {
        ouvintes.add(ouvinte);
    }

    /** Zera a contagem no início de cada passo da demonstração. */
    public synchronized void zerar() {
        contador = 0;
    }

    public synchronized int total() {
        return contador;
    }

    /** Executa algo sem registrar o SQL (ex.: atualizar a tabela da tela). */
    public <T> T silenciosamente(java.util.function.Supplier<T> acao) {
        pausado = true;
        try {
            return acao.get();
        } finally {
            pausado = false;
        }
    }
}
