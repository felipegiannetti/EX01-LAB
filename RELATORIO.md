# Relatório — Roteiro de Laboratório: Threads em Java

Respostas das perguntas de cada parte do roteiro, dos exercícios de fixação
e um resumo pra ficar pronto pra qualquer pergunta oral do professor. O
código tá todo explicado no [README.md](README.md), e a teoria de SO por
trás de tudo isso (processo, thread, escalonamento, modelos de
mapeamento, condição de corrida, deadlock) tá no
[CONCEITOS.md](CONCEITOS.md) — vale ler antes desse relatório se a ideia
é estudar pra prova, não só resolver o roteiro.

## Processo x Thread

O SO cria um processo pra cada programa — no nosso caso, a JVM é o
processo. Toda thread que a gente cria em Java vira uma thread nativa do
SO por baixo (menos as Virtual Threads da Parte E, que são gerenciadas
pela própria JVM, não pelo SO).

| Aspecto | Processo | Thread |
|---|---|---|
| Memória | Espaço de endereçamento próprio | Compartilha a memória do processo pai |
| Criação | Custosa (nova tabela de páginas, novo PID) | Mais leve |
| Comunicação | Precisa de IPC (pipes, sockets) | Variáveis compartilhadas |
| Isolamento | Falha em um processo não derruba outro | Exceção não tratada pode afetar o processo todo |
| Escalonamento | Unidade de alocação de recursos | Unidade que a CPU de fato escalona |

Analogia do guichê (exercício de fixação, item 3): o processo é o prédio
da agência — tem endereço próprio, paredes próprias, e se pegar fogo
(travar), só aquela agência para. As threads são os atendentes lá dentro:
todos usam o mesmo espaço, o mesmo balcão, a mesma memória, então
conseguem se falar direto sem precisar de "correio interno" (isso seria
IPC entre processos). O problema é que, se um atendente surtar e causar
um problema sério (uma exceção não tratada), pode acabar prejudicando o
atendimento dos outros que estão no mesmo prédio. E contratar um
atendente novo é bem mais rápido e barato do que abrir uma agência do
zero — por isso threads são "mais leves" que processos.

## Parte A — extends Thread

O tempo total ficou perto de **1 segundo** (medi: ~1018ms), não 5s.

O motivo é a ordem do código: eu chamo `start()` nas 5 threads primeiro,
todas seguidas, num laço. Só depois, num segundo laço separado, chamo
`join()` em cada uma. Como `start()` já manda o SO criar a thread na hora,
as 5 threads existem e começam a dormir (`Thread.sleep(1000)`) praticamente
juntas — em paralelo. Então o tempo final é o da thread mais devagar, não
a soma das 5.

Se eu tivesse colocado `join()` logo depois de cada `start()`, dentro do
mesmo laço, aí sim ia virar sequencial — a thread 1 teria que terminar
antes de sequer a thread 2 ser criada — e o tempo ia pra perto de 5s. É
basicamente o erro mais comum de quem tá começando com thread: parece que
tá tudo paralelo só porque usou `Thread`, mas se você `join()` cedo demais
a concorrência vira ilusão.

## Parte B — implements Runnable

Só a `AtendimentoRunnable` (Parte B) dá pra herdar de outra classe hoje.

Em Java uma classe só pode dar `extends` numa superclasse só, mas pode
implementar quantas interfaces quiser. A `AtendimentoThread` da Parte A já
usou a única herança que ela tinha disponível pra estender `Thread` — ela
nunca mais vai poder ser, por exemplo, também uma `Funcionario`. Já a
`AtendimentoRunnable` só implementa `Runnable`, então continua livre pra
herdar de qualquer classe que precisar no futuro. É essa regra da
linguagem, e não só "convenção", que sustenta a recomendação de usar
Runnable em vez de Thread.

## Parte C — muitas threads de SO

**Por que criar uma thread de SO é mais caro que criar um objeto comum?**

Um objeto Java normal é só memória no heap da JVM, gerenciada pelo garbage
collector — rápido, não envolve o sistema operacional em nada. Uma thread
de SO é outra história: o kernel precisa reservar uma pilha própria pra
ela (geralmente entre 512KB e 1MB), montar as estruturas de controle do
escalonador (registradores, prioridade, contexto de execução) e registrar
ela na tabela de threads do sistema. Trocar de contexto entre threads
também tem custo — salvar e restaurar tudo isso. Criar objeto Java não
passa nem perto disso.

**O que isso sugere sobre usar 1 thread por requisição num servidor web?**

Que não escala. Se cada requisição HTTP consome uma thread de SO própria,
um servidor recebendo muita conexão simultânea acaba esgotando a memória
reservada pra pilhas de thread e pode lançar
`OutOfMemoryError: unable to create new native thread` — o servidor cai
mesmo sobrando CPU e memória de verdade pra lógica de negócio. Esse foi
exatamente o problema que empurrou o Java pra criar pool de threads
(Parte D) e depois Virtual Threads (Parte E): dá pra ter uma thread por
requisição sem pagar o preço de uma thread de SO por requisição.

## Parte D — ExecutorService

Com pool fixo de 4 threads atendendo 10 clientes, o tempo ficou perto de
**3 segundos** (medi: ~3032ms).

Dá pra calcular isso antes de rodar: 10 tarefas de 1s cada, só 4 threads
disponíveis por vez, então rola em rodadas — clientes 1 a 4 na primeira
rodada (1s), 5 a 8 na segunda (mais 1s), 9 e 10 na terceira (mais 1s). Três
rodadas de 1 segundo, ~3s no total. É o comportamento normal de
`newFixedThreadPool(4)`: só 4 tarefas rodam ao mesmo tempo, o resto fica
na fila esperando uma thread do pool liberar.

## Parte E — Virtual Threads

Não, uma Virtual Thread não é uma thread de Sistema Operacional.

Ela continua sendo representada como um `java.lang.Thread` no código (por
isso `Runnable`, `Thread.sleep()` etc. continuam funcionando igual), mas
quem gerencia ela de verdade é a JVM, não o kernel. Por trás, existe um
número pequeno de threads de SO reais — chamadas de carrier threads — que
emprestam tempo de execução pra Virtual Thread só enquanto ela está
rodando código de fato. Quando ela bloqueia, por exemplo num
`Thread.sleep()` ou numa espera de I/O, a JVM desmonta ela da carrier
thread e libera essa carrier thread pra atender outra Virtual Thread. É
por isso que dá pra ter centenas de milhares dessas coisas "vivas" ao
mesmo tempo com pouquíssimas threads de SO por baixo.

Confirmei isso rodando `Thread.currentThread().isVirtual()` no código, que
deu `true`, e rodando 100 mil tarefas concorrentes — o mesmo volume que
quebrava o modelo da Parte C — sem nenhum OutOfMemoryError, em ~1,4s.

## Exercícios de fixação

**1. Trocar o pool fixo por newCachedThreadPool() — muda o comportamento?**

Muda sim. Tá implementado em
[`parteD/MainCached.java`](src/parteD/MainCached.java). O
`newFixedThreadPool(4)` trava em no máximo 4 threads simultâneas (por
isso as 10 tarefas levaram ~3s, em rodadas). Já o `newCachedThreadPool()`
não tem limite fixo: ele cria uma thread nova pra cada tarefa toda vez que
não sobra nenhuma ociosa pra reaproveitar, e mantém as threads ociosas por
até 60 segundos antes de descartar. Com as 10 tarefas do exercício, isso
criou até 10 threads rodando ao mesmo tempo (`pool-1-thread-1` até
`pool-1-thread-10`) e o tempo caiu pra ~1s.

Só que tem um porém: isso mostra o trade-off na prática. Fixo protege o
sistema de criar thread demais, então é melhor quando a carga é
imprevisível. Cached é mais rápido, mas numa rajada grande de tarefas ele
pode acabar criando tantas threads quanto a Parte C criou — e voltar a
correr o mesmo risco de memória.

**2. Imprimir Thread.currentThread() na Parte E — é uma VirtualThread?**

É sim. A saída que apareceu foi:

```
Thread.currentThread() = VirtualThread[#28]/runnable@ForkJoinPool-1-worker-1
isVirtual() = true
```

O nome da classe já entrega (`VirtualThread`), e o `isVirtual()` retornando
`true` confirma. Se fosse uma thread de SO comum, apareceria como
`Thread[#N,...]` e `isVirtual()` daria `false`. A parte
`@ForkJoinPool-1-worker-1` do nome mostra qual carrier thread (a thread de
SO de verdade) tava executando essa Virtual Thread naquele momento.

**3. Explicar processo x thread com a analogia do guichê**

Já respondi lá em cima, na seção "Processo x Thread".

**4. Qual a abordagem ideal para um servidor com milhares de conexões?**

Virtual Threads (Parte E). Um servidor assim precisa de milhares de linhas
de execução rodando ao mesmo tempo — o ideal, pra manter o código simples,
é uma por conexão. O modelo puro de `extends Thread`/`implements Runnable`
não aguenta, cairia no mesmo problema da Parte C bem antes de chegar em
"milhares". Um `ExecutorService` com pool fixo resolve o problema de
memória, mas troca isso por fila de espera — com pool pequeno e milhares
de requisições, a maioria fica esperando, o que aumenta a latência que o
usuário sente. Virtual Thread resolve os dois lados: código continua
simples (uma por requisição), sem o custo de memória de thread de SO,
porque a JVM reaproveita um número pequeno de carrier threads entre um
monte de Virtual Threads, aproveitando bem os momentos em que elas estão
paradas esperando I/O ou banco de dados.

## O que tem a mais além do roteiro

Implementei as 5 partes e os 2 exercícios pedidos, e ainda fiz mais duas
coisas (pacote [`extra`](src/extra), detalhes no
[README.md](README.md#extras-indo-além-do-roteiro)):

- `CicloDeVidaDemo`: mostra os estados reais de uma thread usando
  `Thread.getState()` — NEW, RUNNABLE, TIMED_WAITING, TERMINATED — em vez
  de deixar isso só no papel/diagrama.
- `Comparativo`: roda a mesma carga de trabalho (2000 tarefas) nos 4
  modelos de concorrência do roteiro ao mesmo tempo e mede tempo total e
  quantas threads de SO cada um usou de pico, usando `ThreadMXBean`. É um
  jeito de comparar tudo lado a lado em vez de só rodar cada parte
  separada com número diferente.

## Checklist

- [x] As 5 partes compilam e rodam sem erro (testado com JDK 26)
- [x] Perguntas de cada parte respondidas
- [x] Consigo explicar processo x thread sem olhar o roteiro
- [x] Extras implementados e testados

---

## Perguntas rápidas pra prova oral

**Qual a diferença entre processo e thread?**
Processo tem memória isolada; thread compartilha a memória do processo
que criou ela. Processo é a unidade que o SO usa pra alocar recursos;
thread é a unidade que o SO de fato escalona na CPU.

**Por que start() e não run()?**
`start()` manda o SO criar uma thread nova de verdade, e é dentro dela que
o `run()` roda. Chamar `run()` direto só executa aquilo como um método
comum, na thread atual, sem criar concorrência nenhuma.

**Por que Runnable é melhor que extends Thread?**
Porque separa a tarefa (o que fazer) de quem executa (a thread) — isso não
gasta a única herança da classe, deixa reaproveitar a mesma tarefa em
threads diferentes, e funciona com `ExecutorService`, que espera um
Runnable, não uma subclasse de Thread.

**Pra que serve join()?**
Faz quem chamou esperar a outra thread terminar antes de continuar. Sem
isso o programa pode seguir (ou até encerrar) antes do trabalho da outra
thread estar pronto.

**Por que criar muita thread de SO quebra o programa?**
Cada uma reserva uma pilha própria (algumas centenas de KB até 1MB) e
estrutura de kernel. Com milhares delas, isso soma vários GB de memória
reservada, até estourar o limite e dar
`OutOfMemoryError: unable to create new native thread`.

**O que o ExecutorService resolve?**
Evita criar thread de SO nova pra cada tarefa. Um número controlado de
threads reais fica sendo reaproveitado, e o que passar disso fica na fila.

**Por que precisa chamar shutdown() no ExecutorService?**
Porque o pool fica esperando tarefa nova pra sempre por padrão.
`shutdown()` avisa que não vai vir mais nada (mas deixa acabar o que já tá
na fila) — sem isso o programa nunca termina sozinho.

**O que é Virtual Thread e por que resolve o problema da Parte C?**
É uma thread leve gerenciada pela JVM, não pelo SO. Várias delas
compartilham poucas threads de SO reais (carrier threads), que só ficam
ocupadas enquanto a Virtual Thread está rodando de verdade — quando ela
bloqueia, a carrier thread libera pra outra. Dá pra ter centenas de
milhares sem estourar a memória do processo.

**Virtual Thread é uma Thread de verdade, na API do Java?**
Sim, `Thread.currentThread()` retorna um `Thread` normal, e `isVirtual()`
diz se é virtual ou de plataforma. O código que você escreve não muda nada
entre os dois modelos.

**Qual a ordem de evolução da concorrência em Java?**
extends Thread (Java 1.0) → implements Runnable (recomendado desde o Java
5) → ExecutorService (Java 5+) → Virtual Threads (Java 21+). Cada geração
resolveu um problema real da anterior: herança única, depois memória,
depois o limite de thread de SO.

**Por que você fez mais do que o roteiro pedia?**
Porque queria provar os conceitos rodando, não só descrever. O
`CicloDeVidaDemo` mostra o ciclo de vida acontecendo de verdade em vez de
ficar só no diagrama, e o `Comparativo` deixa visível numa tabela só a
diferença entre os 4 modelos, em vez de espalhado em experimentos
separados com números diferentes.

**Como o Comparativo mede quantas threads de SO cada modelo usa?**
Com `ManagementFactory.getThreadMXBean().getThreadCount()`, que só conta
threads de plataforma (SO). É por isso que o cenário de Virtual Threads
aparece com pico baixíssimo (~14) — essa API nem enxerga as Virtual
Threads, só as carrier threads reais por trás. Isso já mostra sozinho que
Virtual Thread não é thread de SO.

**Por que o pool fixo foi o mais lento no comparativo?**
Porque ele prioriza controle, não velocidade: só 200 threads pra 2000
tarefas, então o resto espera em fila e roda em rodadas (~2s no total).
Ele nunca ia estourar memória, mas também não ia ser o mais rápido com
tanta tarefa de uma vez — e é justamente essa limitação que Virtual
Thread resolve.
