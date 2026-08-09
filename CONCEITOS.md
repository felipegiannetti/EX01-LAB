## Por que existe processo

Todo programa que roda no computador precisa de um espaço próprio pra
existir: memória pra guardar suas variáveis, um lugar pra saber "onde eu
parei" na execução, arquivos abertos, etc. O Sistema Operacional chama
esse "espaço" de **processo**. Cada vez que você abre um programa (o
Chrome, o VS Code, ou no nosso caso a própria JVM rodando o `.class`), o
SO cria um processo novo pra ele.

O processo é isolado. Ele tem seu próprio espaço de endereçamento de
memória, então um processo não consegue (sem pedir permissão especial ao
SO) ler ou escrever direto na memória de outro processo. Isso é bom pra
segurança e estabilidade — se um processo travar ou corromper a própria
memória, os outros continuam de boa, o SO só mata aquele processo
específico. Mas criar um processo é caro: o SO precisa montar uma tabela
de páginas de memória nova, dar um PID novo, alocar as estruturas de
controle dele.

## Por que existe thread

Só que dentro de um processo, muitas vezes você quer fazer várias coisas
"ao mesmo tempo" sem precisar abrir um processo inteiro novo pra cada
uma. Por exemplo, um navegador quer carregar uma página enquanto ainda
responde ao seu clique no menu. Criar um processo novo pra cada uma
dessas tarefas seria um desperdício gigante de memória e tempo.

A saída foi dividir o processo em **threads**: linhas de execução
independentes que rodam dentro do mesmo processo e compartilham a mesma
memória. Como elas já estão dentro de um processo que já existe, criar
uma thread é bem mais barato que criar um processo — não precisa de
tabela de página nova nem PID novo, só de uma pilha de execução própria e
um registro no escalonador do SO.

O trade-off é que, como as threads compartilham memória, elas também
compartilham os problemas: se uma thread corrompe um dado que outra
thread do mesmo processo está usando, as duas sofrem — não tem aquele
isolamento que existe entre processos.

## Threads de usuário x threads de kernel (por que Virtual Thread existe)

Aqui entra uma parte que geralmente não aparece explicada em curso
básico, mas que é a chave pra entender por que Virtual Thread (Parte E)
é diferente de tudo que veio antes.

Uma thread pode ser gerenciada de duas formas:

- **Thread de kernel** (também chamada de thread de plataforma, ou
  "nativa"): o próprio Sistema Operacional sabe que ela existe, tem ela
  registrada, e o escalonador do SO decide quando ela roda. É cara de
  criar (a tal da pilha reservada + estrutura no kernel que a Parte C
  mostra na prática).
- **Thread de usuário**: existe só dentro do processo, o SO nem sabe que
  ela existe. Quem decide quando ela roda é a própria aplicação (ou, no
  nosso caso, a JVM).

A relação entre quantas threads de usuário existem e quantas threads de
kernel existem por baixo dela tem um nome — é o **modelo de
mapeamento** — e existem três jeitos clássicos de fazer isso:

- **1:1** — cada thread de usuário vira uma thread de kernel. É o modelo
  que o Java sempre usou, desde a `Thread` clássica (Parte A) até o
  `ExecutorService` (Parte D): toda `Thread` do Java corresponde a
  exatamente uma thread do SO. Simples, mas o limite de threads de SO
  vira o limite da sua aplicação (o problema da Parte C).
- **N:1** — várias threads de usuário rodam em cima de uma única thread
  de kernel. Ultraleve, mas se uma trava esperando alguma coisa, trava
  todas as outras junto, porque só existe uma thread de kernel de
  verdade por trás. Não é o modelo usado aqui.
- **M:N** — várias threads de usuário (M) são distribuídas entre um
  número menor de threads de kernel (N), e a "camada de cima" (no nosso
  caso, a JVM) vai trocando qual thread de usuário está montada em cima
  de qual thread de kernel, conforme cada uma bloqueia ou libera. É
  exatamente o modelo das **Virtual Threads**: as threads de kernel reais
  (poucas) são as *carrier threads*, e centenas de milhares de Virtual
  Threads (muitas) se revezam usando elas.

Isso explica por que Virtual Thread não é "uma Thread de SO mais rápida"
— é literalmente outro modelo de mapeamento, resolvendo o mesmo problema
que sistemas operacionais e runtimes de outras linguagens (Go, Erlang)
resolveram décadas atrás com a ideia de threads/corrotinas leves em cima
de poucas threads reais.

## Escalonamento e troca de contexto

Numa máquina com poucos núcleos de CPU e um monte de threads querendo
rodar, quem decide "agora é a sua vez" é o **escalonador** do Sistema
Operacional. Ele dá pra cada thread uma fatia de tempo (quantum) pra
rodar, e quando o tempo acaba (ou a thread bloqueia esperando algo), o
escalonador tira ela da CPU e bota outra pra rodar.

Esse processo de tirar uma thread da CPU e botar outra se chama **troca
de contexto** (context switch), e ele não é de graça: o SO precisa salvar
todo o estado da thread que estava rodando (valores dos registradores,
ponteiro de instrução, etc.) pra poder continuar de onde parou depois, e
carregar o estado da próxima thread. Quanto mais threads de SO
disputando pouca CPU, mais troca de contexto acontece, e mais tempo a
máquina passa "trocando de roupa" em vez de trabalhando de verdade — é
mais um motivo, além da memória, pra threads de SO em excesso deixarem
um programa lento.

## Concorrência não é paralelismo

Essa confusão é super comum. São coisas relacionadas, mas diferentes:

- **Concorrência** é sobre **estrutura**: várias tarefas progredindo ao
  mesmo tempo, do ponto de vista lógico, mesmo que na prática só uma
  esteja rodando em cada instante exato (porque só tem 1 núcleo de CPU,
  por exemplo, e o escalonador fica revezando rapidinho entre elas).
- **Paralelismo** é sobre **execução física simultânea de verdade**: duas
  tarefas rodando ao mesmo tempo, literalmente, em núcleos diferentes da
  CPU.

Dá pra ter concorrência sem paralelismo (um processador de um núcleo só
revezando entre várias threads bem rápido, dando a impressão de
simultaneidade) e dá pra ter paralelismo sem "concorrência" no sentido de
coordenação complexa (duas contas de multiplicação de matriz totalmente
independentes rodando em núcleos diferentes). Nesse laboratório, como a
máquina usada tem vários núcleos, as 5 threads da Parte A rodam com
paralelismo real — mas o conceito de Java (`Thread`, `Runnable`) em si é
sobre concorrência: ele não garante paralelismo, quem decide isso no fim
das contas é o hardware e o escalonador.

## O ciclo de vida de uma thread

Depois que a thread existe, ela fica migrando entre alguns estados até
terminar. No Java isso tem nomes específicos (`Thread.State`), mas a
ideia por trás é a mesma de qualquer SO:

- **Pronta pra rodar** (`RUNNABLE` no Java): a thread quer executar, só
  está esperando o escalonador dar a vez dela.
- **Rodando**: está de fato usando a CPU nesse instante (no Java isso
  também conta como `RUNNABLE` — a API não distingue "pronta" de
  "rodando agora").
- **Bloqueada/esperando**: a thread não pode continuar até algo
  acontecer — pode ser esperar um tempo (`Thread.sleep`), esperar outra
  thread terminar (`join()`), ou esperar um recurso compartilhado ficar
  livre (`BLOCKED`, esperando um `synchronized`).
- **Terminada**: a thread já rodou tudo que tinha que rodar e não volta
  mais.

O diagrama completo com as transições do Java (`NEW → RUNNABLE →
TIMED_WAITING/WAITING/BLOCKED → TERMINATED`) tá no
[README.md](README.md#ciclo-de-vida-de-uma-thread), junto com o código
que prova isso rodando de verdade
([`extra/CicloDeVidaDemo.java`](src/extra/CicloDeVidaDemo.java)).

## Os problemas clássicos de programar com threads

O nosso laboratório evita esse problema de propósito (nenhuma das partes
usa uma variável compartilhada sendo alterada por várias threads ao mesmo
tempo), mas é importante saber que ele existe, porque é provavelmente a
próxima coisa que a disciplina vai cobrar depois desse roteiro.

Quando duas ou mais threads **leem e escrevem a mesma variável** sem
nenhum tipo de controle, o resultado final pode depender da ordem exata
em que o escalonador decidiu rodar cada uma — e essa ordem muda a cada
execução. Isso se chama **condição de corrida** (race condition), e é um
dos bugs mais chatos de debugar porque às vezes o programa funciona 999
vezes e falha só na execução 1000.

O trecho de código onde a variável compartilhada é acessada se chama
**seção crítica**. A forma clássica de proteger uma seção crítica em Java
é a palavra-chave `synchronized`, que garante **exclusão mútua**: só uma
thread por vez pode estar dentro daquele trecho. É justamente por causa
disso que existe o estado `BLOCKED` no ciclo de vida — é a thread
esperando a vez de entrar numa seção `synchronized` que outra thread já
está ocupando.

Só que resolver concorrência com locks (`synchronized`) traz um risco
novo: o **deadlock**. Acontece quando a thread A está esperando um
recurso que a thread B segura, e a thread B está esperando um recurso que
a thread A segura — as duas travam pra sempre, esperando uma a outra.
Existe também a **starvation** (inanição): uma thread que nunca consegue
a vez de rodar porque o escalonador (ou a lógica do programa) sempre
prioriza outras threads antes dela.

Nenhum desses problemas aparece nas Partes A–E porque as tarefas ali só
imprimem no console e dormem (`Thread.sleep`) — não têm estado
compartilhado sendo escrito por várias threads ao mesmo tempo. Mas é
importante saber nomear esses conceitos, porque é bem provável que uma
próxima aula ou uma pergunta de prova entre nesse assunto.

## Por que threads de SO custam caro (revisão rápida)

Já foi respondido no RELATORIO em cima da Parte C, mas juntando com a
teoria de cima: uma thread de SO custa porque ela é gerenciada pelo
**kernel**, que precisa reservar pilha própria e manter estrutura de
controle pra ela poder ser escalonada e sofrer troca de contexto. Uma
thread de usuário (como a Virtual Thread) não tem esse custo todo porque
quem cuida dela é a própria aplicação/JVM, não o kernel — só as poucas
carrier threads por baixo é que realmente custam caro pro SO.

## A evolução da concorrência em Java, agora do ponto de vista da teoria

Juntando tudo:

1. **`extends Thread` / `implements Runnable`** (Partes A e B): mapeamento
   1:1, cada `Thread` do Java é uma thread de kernel. Simples de entender,
   mas caro em escala.
2. **`ExecutorService`** (Parte D): continua sendo 1:1 por baixo (cada
   thread do pool ainda é uma thread de kernel), só que ao invés de criar
   uma pra cada tarefa, um número fixo delas é **reaproveitado**. Resolve
   o problema de escala trocando por fila de espera quando a demanda
   passa do tamanho do pool.
3. **Virtual Threads** (Parte E): muda de modelo pra M:N. Agora existem
   muito mais threads de usuário (virtuais) do que threads de kernel
   (carrier threads), e a JVM gerencia esse mapeamento dinamicamente,
   trocando qual Virtual Thread está "montada" em cima de qual carrier
   thread sempre que uma bloqueia.

## Tabela: conceito de teoria → onde aparece no projeto

| Conceito | Onde ver na prática |
|---|---|
| Processo x thread (memória compartilhada) | [Revisão conceitual no RELATORIO.md](RELATORIO.md#processo-x-thread) |
| Mapeamento 1:1 | Partes A, B, C, D — toda `Thread` vira thread de SO |
| Mapeamento M:N | Parte E — [`parteE/Main.java`](src/parteE/Main.java), Virtual Threads sobre carrier threads |
| Custo de criação de thread de SO | Parte C — [`parteC/Main.java`](src/parteC/Main.java), 10 mil threads criadas |
| Escalonamento / pool reaproveitando threads | Parte D — [`parteD/Main.java`](src/parteD/Main.java) |
| Ciclo de vida da thread (estados) | [`extra/CicloDeVidaDemo.java`](src/extra/CicloDeVidaDemo.java) |
| `start()` x `run()` | Partes A e B — ver [README.md](README.md) |
| `join()` (sincronização simples) | Todas as partes que esperam threads terminarem |
| Comparação de custo entre os 4 modelos | [`extra/Comparativo.java`](src/extra/Comparativo.java) |

## Perguntas rápidas pra testar se entendeu a teoria (sem olhar a resposta)

1. Por que uma thread compartilha memória com outras threads do mesmo
   processo, mas um processo não compartilha memória com outro processo?
2. No modelo M:N, o que exatamente é uma "carrier thread"?
3. Se um programa tem 10 threads rodando numa máquina de 1 núcleo só, ele
   tem concorrência, paralelismo, os dois ou nenhum dos dois?
4. O que diferencia uma condição de corrida de um deadlock?
5. Por que `Thread.sleep()` faz a thread sair do estado `RUNNABLE` mas não
   é a mesma coisa que `BLOCKED`?

(Respostas: 1 — porque threads nascem *dentro* de um processo já
existente e usam o espaço de memória que o SO já reservou pra aquele
processo, enquanto processos são isolados de propósito, cada um com sua
própria tabela de páginas. 2 — é uma thread de kernel real, das poucas
que existem, que "empresta" tempo de CPU pras Virtual Threads quando
elas precisam rodar código de verdade. 3 — só concorrência: as 10 threads
progridem de forma intercalada (o escalonador revezando rapidinho entre
elas), mas nunca duas rodando ao mesmo tempo de verdade, porque só existe
1 núcleo. 4 — condição de corrida é um resultado errado por causa da
ordem de acesso a um dado compartilhado; deadlock é as threads travarem
de vez, esperando uma pela outra, sem chegar a resultado nenhum. 5 —
`BLOCKED` é especificamente esperar um lock `synchronized` liberar;
`Thread.sleep()` gera `TIMED_WAITING`, que é a thread escolhendo esperar
por tempo, não disputando um recurso com outra thread.)
