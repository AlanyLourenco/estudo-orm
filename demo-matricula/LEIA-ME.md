# Demonstração de ORM · Matrícula escolar

Aplicação Java (Swing + Hibernate 7) para mostrar, ao vivo, o SQL que o ORM gera.
Janela: **roteiro** à esquerda, **objetos** no centro, **código Java + SQL enviado ao banco** à direita.

## Como abrir

1. Dê dois cliques em `executar.bat`: ele compila e conecta direto no PostgreSQL com os dados de `db.properties`.
2. A tela de conexão só aparece se não houver senha salva ou se a conexão falhar (senha trocada, PostgreSQL desligado).
   Nela, deixe marcado "lembrar a senha" para não ser perguntado de novo.
3. As tabelas são **apagadas e recriadas** a cada abertura, com 10 turmas, 4 disciplinas e 30 alunos.
   Toda demonstração começa igual. Ao fechar, os dados continuam no banco (dá para ver no pgAdmin).

Teste rápido sem janela e sem PostgreSQL: `executar.bat --teste` (roda o roteiro no console, em H2).

## Ver as tabelas no pgAdmin

`Servers → PostgreSQL 17 → Databases → escola_orm → Schemas → public → Tables`.
Clique com o botão direito em `aluno` e escolha **View/Edit Data → All Rows**.
Depois de cada passo, aperte **F5** na grade do pgAdmin para ver a mudança.

## Roteiro (uns 5 minutos)

Na coluna da esquerda, clique no passo da lista. Embaixo aparecem **só os campos daquele passo** e o botão que o executa.

| Passo (na lista) | Campos que usa | Botão | O que mostrar | Slide |
|---|---|---|---|---|
| 1 · Cadastrar aluno | Nome, Turma, Disciplinas | **Salvar** | Aluno novo: `INSERT` em `aluno` + 1 por disciplina em `aluno_disciplina` (o id gerado volta para o objeto). Aluno que já existe (mesmo nome): só os `INSERT`s das disciplinas novas. | 10 |
| 2 · Buscar pelo id | id do aluno | **Buscar pelo id** | 1 `SELECT` com `LEFT JOIN turma`: `@ManyToOne` é EAGER por padrão. | 14 |
| 3 · Trocar o nome sem salvar | id do aluno, Novo nome | **Trocar o nome** | `SELECT` + `UPDATE`, sem nenhum `save()`: *dirty checking*. | 17 |
| 4 · Listar turmas | nenhum | **Listar turmas e alunos** | O placar marca **11**: 1 consulta das turmas + 1 por turma (**N+1**). | 29 e 36 |
| 5 · Listar com JOIN FETCH | nenhum | **Listar com JOIN FETCH** | Mesmo resultado, placar **1**. | 36 |
| + · Matricular em disciplina | id do aluno, Disciplina | **Matricular** | Só `getDisciplinas().add(d)`: o commit gera 1 `INSERT` em `aluno_disciplina`. Se o aluno já cursa a disciplina, nenhum INSERT. | 25 |
| + · Remover pelo id | id do aluno | **Remover pelo id** | O ORM apaga antes as linhas em `aluno_disciplina`. | 17 |

No passo 1, o aluno é reconhecido pelo e-mail (gerado a partir do nome). Salvar "Ana Souza" com Banco de Dados
e depois "Ana Souza" com POO deixa **a mesma Ana** nas duas disciplinas. A turma não muda: cada aluno tem uma só.
"Matricular" faz o mesmo, mas escolhendo o aluno pelo id.

O diagrama das classes e das tabelas está em `docs/diagrama-uml.pdf` (também em `.png` e `.html`).

Dicas durante a apresentação:
- A busca, a troca de nome e a remoção são **pelo id**. Clicar numa linha da tabela preenche o id desses passos.
  Depois do passo 1, o aluno recém-criado já fica selecionado.
- **Ctrl +** e **Ctrl −** aumentam e diminuem o texto (bom para o projetor).
- **Limpar SQL** deixa o painel da direita vazio antes de um passo importante.
- O SQL aparece com `?` no lugar dos valores: são parâmetros vinculados (proteção contra SQL injection, slide 37).

## Estrutura

```
src/
  modelo/        Aluno, Turma, Disciplina  (as entidades, com as anotações JPA)
  persistencia/  JPA (fábrica), ConfiguracaoBanco (db.properties), MonitorSQL (captura o SQL)
  negocio/       Escola (os passos da demonstração)
  visao/         JanelaPrincipal, DialogoConexao, PainelSQL, Tema
  META-INF/persistence.xml
lib/             Hibernate 7.4, driver PostgreSQL, H2
```

`db.properties` guarda os dados de conexão. A senha só é salva se a caixa "lembrar a senha" for marcada.
