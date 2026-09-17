package negocio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import modelo.Aluno;
import modelo.Disciplina;
import modelo.Turma;
import persistencia.JPA;
import persistencia.MonitorSQL;

/**
 * As operações da demonstração. Cada uma devolve o que a aplicação "enxerga"
 * (o mundo dos objetos); o SQL correspondente é capturado pelo MonitorSQL.
 */
public class Escola {

    /** Resultado de um passo: linhas de texto e, se houver, o id do aluno envolvido. */
    public record Resultado(List<String> linhas, Long idAluno) {
        public Resultado(List<String> linhas) {
            this(linhas, null);
        }

        static Resultado de(String... linhas) {
            return new Resultado(List.of(linhas));
        }
    }

    /** Linha da tabela "Mundo dos objetos". */
    public record LinhaAluno(Long id, String nome, String email, String turma, String disciplinas) { }

    /** Item de lista (turma ou disciplina) para os campos da tela. */
    public record Item(Long id, String nome) {
        @Override
        public String toString() { return nome; }
    }

    private static final String[] TURMAS = {
        "Turma A", "Turma B", "Turma C", "Turma D", "Turma E",
        "Turma F", "Turma G", "Turma H", "Turma I", "Turma J"
    };
    private static final String[] DISCIPLINAS = { "Banco de Dados", "POO", "Estruturas de Dados", "Redes" };
    private static final String[] NOMES = {
        "Bruno Lima", "Carla Dias", "Diego Rocha", "Elisa Prado", "Fábio Nunes", "Gabriela Reis",
        "Heitor Melo", "Isabela Costa", "João Pedro", "Karina Alves", "Lucas Faria", "Marina Souza",
        "Nicolas Teixeira", "Olívia Ramos", "Paulo Henrique", "Quésia Moura", "Rafael Pires", "Sofia Barros",
        "Tiago Mendes", "Úrsula Gomes", "Vitor Hugo", "Wesley Martins", "Yasmin Freitas", "Zeca Andrade",
        "Amanda Lopes", "Bernardo Cruz", "Cecília Duarte", "Daniel Viana", "Estela Campos", "Felipe Moraes"
    };

    // ------------------------------------------------------------------ dados iniciais

    /** 10 turmas, 4 disciplinas e 30 alunos (3 por turma, 2 disciplinas cada). */
    public static void popularDados() {
        MonitorSQL.INSTANCIA.silenciosamente(() -> emTransacao(em -> {
            List<Turma> turmas = new ArrayList<>();
            for (String t : TURMAS) {
                Turma turma = new Turma(t);
                em.persist(turma);
                turmas.add(turma);
            }
            List<Disciplina> disciplinas = new ArrayList<>();
            for (String d : DISCIPLINAS) {
                Disciplina disciplina = new Disciplina(d);
                em.persist(disciplina);
                disciplinas.add(disciplina);
            }
            for (int i = 0; i < NOMES.length; i++) {
                Aluno a = new Aluno(NOMES[i], email(NOMES[i]));
                a.setTurma(turmas.get(i / 3));
                a.getDisciplinas().add(disciplinas.get(i % 4));
                a.getDisciplinas().add(disciplinas.get((i + 1) % 4));
                em.persist(a);
            }
            return null;
        }));
    }

    public static String email(String nome) {
        String base = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().replace(' ', '.');
        return base + "@uni.br";
    }

    // ------------------------------------------------------------------ consultas da tela (sem registrar SQL)

    public static List<LinhaAluno> alunosParaTabela() {
        return MonitorSQL.INSTANCIA.silenciosamente(() -> consultar(em -> {
            List<Aluno> alunos = em.createQuery(
                    "SELECT DISTINCT a FROM Aluno a LEFT JOIN FETCH a.turma LEFT JOIN FETCH a.disciplinas ORDER BY a.id",
                    Aluno.class).getResultList();
            List<LinhaAluno> linhas = new ArrayList<>();
            for (Aluno a : alunos) {
                List<String> ds = a.getDisciplinas().stream().map(Disciplina::getNome).sorted().toList();
                linhas.add(new LinhaAluno(a.getId(), a.getNome(), a.getEmail(),
                        a.getTurma() == null ? "—" : a.getTurma().getNome(), String.join(", ", ds)));
            }
            return linhas;
        }));
    }

    public static List<Item> turmas() {
        return MonitorSQL.INSTANCIA.silenciosamente(() -> consultar(em ->
                em.createQuery("SELECT new negocio.Escola$Item(t.id, t.nome) FROM Turma t ORDER BY t.id", Item.class)
                        .getResultList()));
    }

    public static List<Item> disciplinas() {
        return MonitorSQL.INSTANCIA.silenciosamente(() -> consultar(em ->
                em.createQuery("SELECT new negocio.Escola$Item(d.id, d.nome) FROM Disciplina d ORDER BY d.id", Item.class)
                        .getResultList()));
    }

    // ------------------------------------------------------------------ os passos da demonstração

    public static final String CODIGO_CRIAR = """
            Aluno a = buscarPorEmail(email); // SELECT
            if (a == null) {                 // não existe
                a = new Aluno(nome, email);
                a.setTurma(turma);
                em.persist(a);               // INSERT aluno
            }
            a.getDisciplinas().add(d);       // INSERT junção
            tx.commit();""";

    /** O que aconteceu no cadastro (para montar a mensagem depois do commit). */
    private record Cadastro(Aluno aluno, boolean novo, List<String> adicionadas, List<String> jaCursava,
            boolean outraTurma) { }

    /**
     * Cadastra o aluno nas disciplinas marcadas. Se já existe um aluno com esse
     * e-mail, não cria outro: só acrescenta as disciplinas que ele ainda não cursa.
     */
    public static Resultado cadastrarAluno(String nome, String email, Long turmaId, List<Long> disciplinaIds) {
        return emTransacao(em -> {
            List<Aluno> existentes = em.createQuery("SELECT a FROM Aluno a WHERE a.email = :email", Aluno.class)
                    .setParameter("email", email).getResultList();
            boolean novo = existentes.isEmpty();
            Aluno aluno;
            boolean outraTurma = false;
            if (novo) {
                aluno = new Aluno(nome, email);
                if (turmaId != null) {
                    aluno.setTurma(em.find(Turma.class, turmaId));
                }
                em.persist(aluno);
            } else {
                aluno = existentes.get(0);
                outraTurma = aluno.getTurma() != null && turmaId != null && !aluno.getTurma().getId().equals(turmaId);
            }
            List<String> adicionadas = new ArrayList<>();
            List<String> jaCursava = new ArrayList<>();
            for (Long id : disciplinaIds) {
                Disciplina d = em.find(Disciplina.class, id);
                (aluno.getDisciplinas().add(d) ? adicionadas : jaCursava).add(d.getNome());
            }
            return new Cadastro(aluno, novo, adicionadas, jaCursava, outraTurma);
        }, c -> {
            List<String> linhas = new ArrayList<>();
            Aluno a = c.aluno();
            if (c.novo()) {
                linhas.add("Aluno novo salvo: Aluno { id = " + a.getId() + ", nome = \"" + a.getNome() + "\" }");
                linhas.add("O id " + a.getId() + " foi gerado pelo banco e copiado para o objeto.");
            } else {
                linhas.add(a.getNome() + " já existia (id " + a.getId() + ", " + a.getEmail() + ").");
                linhas.add("Nenhum aluno novo foi criado: só as disciplinas foram acrescentadas.");
                if (c.outraTurma()) {
                    linhas.add("Turma mantida: " + a.getTurma().getNome() + " (cada aluno tem uma turma só).");
                }
            }
            linhas.add(c.adicionadas().isEmpty()
                    ? "Nenhuma disciplina nova."
                    : "Disciplinas acrescentadas: " + String.join(", ", c.adicionadas())
                            + " → uma linha em aluno_disciplina para cada.");
            if (!c.jaCursava().isEmpty()) {
                linhas.add("Já cursava (ignoradas): " + String.join(", ", c.jaCursava()) + ".");
            }
            return new Resultado(linhas, a.getId());
        });
    }

    public static final String CODIGO_BUSCAR = """
            Aluno a = em.find(Aluno.class, id);
            a.getNome();
            a.getTurma().getNome();   // turma veio junto (EAGER)""";

    public static Resultado buscarAluno(Long id) {
        return consultar(em -> {
            Aluno a = em.find(Aluno.class, id);
            if (a == null) {
                return Resultado.de("Nenhum aluno com id " + id + ".");
            }
            return Resultado.de(
                    "Aluno { id = " + a.getId() + " }",
                    "  nome  = \"" + a.getNome() + "\"",
                    "  email = \"" + a.getEmail() + "\"",
                    "  turma = Turma { nome = \"" + (a.getTurma() == null ? "—" : a.getTurma().getNome()) + "\" }",
                    "Uma linha da tabela virou um objeto preenchido.");
        });
    }

    public static final String CODIGO_RENOMEAR = """
            tx.begin();
            Aluno a = em.find(Aluno.class, id);
            a.setNome(novoNome);   // só isso: nenhum save()
            tx.commit();           // dirty checking → UPDATE""";

    public static Resultado renomearAluno(Long id, String novoNome) {
        return emTransacao(em -> {
            Aluno a = em.find(Aluno.class, id);
            if (a == null) {
                return Resultado.de("Nenhum aluno com id " + id + ".");
            }
            String antigo = a.getNome();
            a.setNome(novoNome);
            return Resultado.de(
                    "\"" + antigo + "\"  →  \"" + novoNome + "\"",
                    "Nenhum método de salvar foi chamado.",
                    "No commit, o ORM percebeu a mudança e gerou o UPDATE sozinho.");
        });
    }

    public static final String CODIGO_MATRICULAR = """
            tx.begin();
            Aluno a = em.find(Aluno.class, id);
            Disciplina d = em.find(Disciplina.class, disciplinaId);
            a.getDisciplinas().add(d);   // só mexe na coleção
            tx.commit();   // → INSERT em aluno_disciplina""";

    /** Matricula um aluno que já existe em mais uma disciplina (N:N). */
    public static Resultado matricular(Long alunoId, Long disciplinaId) {
        return emTransacao(em -> {
            Aluno a = em.find(Aluno.class, alunoId);
            if (a == null) {
                return Resultado.de("Nenhum aluno com id " + alunoId + ".");
            }
            Disciplina d = em.find(Disciplina.class, disciplinaId);
            if (!a.getDisciplinas().add(d)) {
                return Resultado.de(
                        a.getNome() + " já cursa " + d.getNome() + ".",
                        "A coleção é um Set (não aceita repetição) e a tabela aluno_disciplina",
                        "tem chave (aluno_id, disciplina_id): nenhum INSERT foi gerado.");
            }
            return Resultado.de(
                    a.getNome() + " agora cursa " + d.getNome() + ".",
                    "O código só adicionou a disciplina na coleção do objeto;",
                    "no commit, o ORM gerou o INSERT em aluno_disciplina.");
        });
    }

    public static final String CODIGO_N_MAIS_1 = """
            List<Turma> turmas = em.createQuery(
                "SELECT t FROM Turma t", Turma.class)
                .getResultList();            // 1 consulta
            for (Turma t : turmas) {
                t.getAlunos().size();        // +1 por turma!
            }""";

    public static Resultado listarTurmasNmais1() {
        return consultar(em -> {
            List<Turma> turmas = em.createQuery("SELECT t FROM Turma t ORDER BY t.id", Turma.class).getResultList();
            List<String> linhas = new ArrayList<>();
            for (Turma t : turmas) {
                linhas.add(t.getNome() + ": " + t.getAlunos().size() + " alunos");
            }
            return new Resultado(linhas);
        });
    }

    public static final String CODIGO_JOIN_FETCH = """
            List<Turma> turmas = em.createQuery(
                "SELECT DISTINCT t FROM Turma t " +
                "LEFT JOIN FETCH t.alunos", Turma.class)
                .getResultList();            // 1 consulta só
            for (Turma t : turmas) {
                t.getAlunos().size();        // já está na memória
            }""";

    public static Resultado listarTurmasJoinFetch() {
        return consultar(em -> {
            List<Turma> turmas = em.createQuery(
                    "SELECT DISTINCT t FROM Turma t LEFT JOIN FETCH t.alunos ORDER BY t.id", Turma.class)
                    .getResultList();
            List<String> linhas = new ArrayList<>();
            for (Turma t : turmas) {
                linhas.add(t.getNome() + ": " + t.getAlunos().size() + " alunos");
            }
            return new Resultado(linhas);
        });
    }

    public static final String CODIGO_REMOVER = """
            tx.begin();
            Aluno a = em.find(Aluno.class, id);
            em.remove(a);          // vira DELETE no commit
            tx.commit();""";

    public static Resultado removerAluno(Long id) {
        return emTransacao(em -> {
            Aluno a = em.find(Aluno.class, id);
            if (a == null) {
                return Resultado.de("Nenhum aluno com id " + id + ".");
            }
            em.remove(a);
            return Resultado.de(
                    "Aluno \"" + a.getNome() + "\" removido.",
                    "O ORM apagou antes as linhas dele em aluno_disciplina.");
        });
    }

    // ------------------------------------------------------------------ apoio

    private static <T> T consultar(Function<EntityManager, T> acao) {
        EntityManager em = JPA.novoEntityManager();
        try {
            return acao.apply(em);
        } finally {
            em.close();
        }
    }

    private static <T> T emTransacao(Function<EntityManager, T> acao) {
        return emTransacao(acao, Function.identity());
    }

    /** Executa em transação; "depois" roda após o commit (quando o id já existe). */
    private static <T, R> R emTransacao(Function<EntityManager, T> acao, Function<T, R> depois) {
        EntityManager em = JPA.novoEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T valor = acao.apply(em);
            tx.commit();
            return depois.apply(valor);
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
