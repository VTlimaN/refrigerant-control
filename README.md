# Controle de Gases Refrigerantes

Este projeto é uma aplicação Java educacional que, em etapas futuras, substituirá uma planilha usada para controlar o consumo de gases refrigerantes. O princípio do produto é: **“Completo internamente, simples externamente.”**

## Objetivo do primeiro marco

O primeiro marco existe somente para confirmar que o ambiente de desenvolvimento e a base técnica funcionam corretamente. Ele comprova:

- compilação com Java 25;
- execução do Maven Wrapper;
- testes automatizados;
- geração de um JAR executável;
- inicialização do Spring Boot e do Tomcat embarcado;
- roteamento com Spring MVC;
- renderização de HTML com Thymeleaf;
- serialização de JSON.

O primeiro marco não continha banco de dados, autenticação, regras de negócio, painel, cadastro de gases, cadastro de cilindros ou outras telas operacionais.

## Milestone 2B.1 — Fundações do domínio

O Milestone 2B.1 adiciona um primeiro recorte do domínio como Java puro, sem dependência de Spring. O pacote `dev.sasser.refrigerantcontrol.domain` contém:

- `Weight`, com valores em quilogramas representados por `BigDecimal` e igualdade numérica;
- `SealNumber`, como identidade imutável do cilindro;
- `RefrigerantGas`, limitado aos nomes operacionais atualmente confirmados;
- `Cylinder`, com associação imutável ao refrigerante e registro separado do peso bruto inicial;
- `UsageActivity` e `ActivityStatus`, com a transição determinística de `AWAITING_RETURN_WEIGHT` para `COMPLETED`;
- `UsageActivityStarter`, que bloqueia uma segunda atividade pendente quando recebe a coleção completa de atividades relevantes.

Os instantes são fornecidos explicitamente às operações do domínio, que não consulta o relógio do sistema. Os testes do domínio usam JUnit diretamente, sem iniciar o contexto Spring.

Esta etapa não implementa persistência, transações, unicidade global de lacres, interface operacional, catálogo técnico editável, cilindros `EMPTY` nem o restante do Milestone 2B. Essas capacidades continuam adiadas conforme as perguntas abertas na documentação do domínio.

## Milestone 2B.2 — Casos de uso do fluxo normal

O Milestone 2B.2 adiciona uma camada de aplicação também escrita em Java puro. `CylinderUseCases` coordena o cadastro de cilindros e o registro separado do peso bruto inicial. `UsageActivityUseCases` coordena o início e a conclusão da atividade pendente, reutilizando as regras existentes no domínio.

As entradas da aplicação usam `String` e `BigDecimal`. As saídas são registros imutáveis, `CylinderResult` e `UsageActivityResult`, que não expõem as entidades mutáveis do domínio. Os instantes de início e conclusão são obtidos de um `Clock` fornecido ao caso de uso e continuam sendo passados explicitamente ao domínio.

As interfaces `CylinderStore` e `UsageActivityStore` definem operações específicas para cadastro único, atualização do cilindro, início atômico e conclusão da atividade pendente. Os adaptadores `InMemoryCylinderStore` e `InMemoryUsageActivityStore` permitem executar e testar essas operações sem Spring e sem banco de dados. Eles guardam snapshots imutáveis, devolvem reconstruções independentes e preservam o histórico de atividades concluídas usado na validação de um novo início.

Esses adaptadores são temporários, não duráveis e locais ao processo. A atomicidade e a unicidade valem somente para chamadas que compartilham a mesma instância do adaptador na mesma JVM. Não há garantia distribuída nem transação de banco de dados.

## Milestone 2C.1 — Raiz de composição Spring

O `ApplicationConfiguration` é a raiz de composição Spring desta etapa. Ele configura exatamente um bean de `CylinderStore`, `UsageActivityStore`, `Clock`, `CylinderUseCases` e `UsageActivityUseCases`. Os stores usam `InMemoryCylinderStore` e `InMemoryUsageActivityStore`, enquanto o relógio de produção usa `Clock.systemUTC()`.

Os beans usam o escopo singleton padrão: existe uma instância de cada store por contexto da aplicação, compartilhada pelos casos de uso desse contexto. O estado continua somente na memória do processo e é perdido quando o contexto ou o processo termina, inclusive após uma reinicialização. A atomicidade continua limitada às chamadas que compartilham a mesma instância do adaptador na mesma JVM; não há persistência em banco de dados, garantia entre processos ou distribuída, nem gerenciamento de transações pelo Spring.

Esta composição disponibiliza os casos de uso como beans. O Milestone 2C.2 conecta somente o cadastro de cilindros e o peso bruto inicial a um primeiro fluxo HTTP; as atividades continuam sem interface operacional.

## Milestone 2C.2 — Fluxo web de cadastro de cilindro e peso bruto inicial

O primeiro fluxo operacional está disponível em `GET /cylinders`. A mesma página contém dois formulários simples: `POST /cylinders` cadastra um cilindro por lacre e gás operacional, e `POST /cylinders/initial-weight` registra separadamente seu peso bruto inicial.

O lacre não vazio é preservado exatamente como informado. O gás é selecionado entre os sete nomes operacionais confirmados: `R410A`, `R32`, `R-22`, `R407C`, `R134A`, `R404` e `141B`. O peso é recebido como texto na fronteira web, aceita vírgula ou ponto decimal e é convertido diretamente para `BigDecimal`, sem `double`, arredondamento ou alteração de escala.

Depois de um cadastro bem-sucedido, o controller usa POST/Redirect/GET e prepara o formulário de peso inicial com o mesmo lacre. Erros de formulário e de negócio são apresentados em português sem expor mensagens técnicas. A página informa claramente que os dados existem somente na memória do processo e são completamente perdidos quando a aplicação é reiniciada.

Este fluxo não inclui início ou retorno de atividades, histórico, lista de cilindros, banco de dados, autenticação nem uma interface operacional completa. A atomicidade continua restrita às chamadas que compartilham a mesma instância do adaptador em memória na mesma JVM.

## Milestone 2C.3A — Contrato obrigatório do local da atividade

O início de uma `UsageActivity` agora exige `activityLocation` como texto livre não vazio. Valores nulos, vazios ou formados somente por espaços são rejeitados pelo domínio. Todo valor não vazio é preservado exatamente, inclusive espaços externos, capitalização, acentos e pontuação; não existe normalização, limite arbitrário, endereço estruturado nem objeto de valor adicional.

`UsageActivityUseCases` diferencia a ausência do peso bruto inicial de uma atividade pendente por exceções específicas da aplicação. A detecção da atividade pendente ocorre dentro da operação atômica do `UsageActivityStore`, enquanto `UsageActivityStarter` continua protegendo a mesma regra no domínio. Os snapshots em memória preservam o local em atividades pendentes e concluídas, sem introduzir persistência durável ou garantia entre processos.

Esta etapa altera somente o contrato interno de domínio e aplicação. Ainda não existe controller, formulário, template ou rota HTTP para iniciar atividades.

## Milestone 2C.3B — Fluxo web de início de atividade

O início de atividade está disponível em `GET /activities/start`. A página apresenta somente os três campos necessários para a operação diária: número do lacre, peso bruto de saída em quilogramas e local da atividade. O envio usa `POST /activities/start` e, quando concluído com sucesso, aplica POST/Redirect/GET para voltar à rota fixa de início sem repetir a operação ao atualizar a página.

O lacre e o local não vazios são entregues ao caso de uso exatamente como informados, preservando espaços externos e internos, capitalização, acentos e pontuação. Somente o texto do peso remove espaços externos; ele aceita vírgula ou ponto como separador decimal e é convertido diretamente para `BigDecimal`, mantendo a escala e sem usar `double` ou arredondamento. O gás é obtido internamente do cilindro cadastrado, o instante `startedAt` é gerado automaticamente por `UsageActivityUseCases` com seu `Clock` injetado e a situação inicial é automaticamente `AWAITING_RETURN_WEIGHT`.

O cilindro precisa existir e ter o peso bruto inicial registrado. Também não pode haver outra atividade do mesmo cilindro aguardando o peso de retorno. Depois do registro bem-sucedido do peso inicial, o fluxo redireciona para a página de início da atividade com o lacre preenchido por `RedirectAttributes`. Falhas de formulário e de negócio permanecem na página, preservam as entradas e exibem mensagens fixas em português sem revelar detalhes técnicos.

As atividades e os cilindros continuam armazenados somente na memória da JVM. Todos esses dados são perdidos quando a aplicação ou seu contexto é reiniciado. Esta etapa não oferece interface de retorno, interface de conclusão, interface de consumo, histórico de atividades, persistência, autenticação nem uma interface operacional completa.

## Milestone 2C.4A — Contrato de aplicação para conclusão de atividade

A conclusão da atividade pendente agora exige uma confirmação explícita quando o peso bruto de retorno é numericamente igual ao peso bruto de saída. Essa igualdade produz consumo zero. Uma tentativa sem confirmação não altera a atividade: ela continua aguardando o peso de retorno, sem peso de retorno, instante de conclusão ou consumo registrados.

Quando o consumo zero é confirmado, a mesma atividade passa para `COMPLETED`. Os pesos brutos de saída e retorno permanecem registrados com seus valores e escalas originais, `zeroConsumptionConfirmed` registra a confirmação com valor `true` e `completedAt`, obtido do `Clock` injetado, registra o instante do evento confirmado. Em uma conclusão com consumo diferente de zero, `zeroConsumptionConfirmed` sempre permanece `false`, mesmo que o chamador forneça confirmação.

Pesos de retorno negativos e pesos de retorno maiores que o peso de saída agora produzem falhas de aplicação tipadas e distintas. A validação da relação entre os pesos e da confirmação ocorre dentro da operação atômica do `UsageActivityStore`; uma falha não substitui o snapshot pendente e permite uma nova tentativa válida. As regras continuam protegidas também pelo domínio.

Este marco altera somente o contrato interno de domínio, aplicação e armazenamento em memória. Ainda não existe rota, controller, formulário, template ou navegação para registrar o retorno. O armazenamento continua temporário na memória da JVM, e todos os dados são perdidos quando a aplicação ou seu contexto é reiniciado. Não há persistência, histórico, autenticação nem interface operacional de conclusão.

## Tecnologias

### Java 25

Java é a linguagem da aplicação. O projeto compila para Java 25 sem recursos de pré-visualização.

### Maven e Maven Wrapper

Maven lê o arquivo `pom.xml`, resolve dependências, compila o código, executa os testes e empacota a aplicação. O Maven Wrapper fixa a versão Maven 3.9.16 para que o projeto use a mesma ferramenta no Windows e no Debian, mesmo sem uma instalação global do Maven.

### Spring Boot

Spring Boot configura e inicia a aplicação Java. O plugin do Spring Boot também transforma o resultado da compilação em um JAR executável.

### Spring MVC

Spring MVC recebe as requisições HTTP. Ele atende a página inicial, o status, o fluxo de cadastro em `/cylinders` e o início de atividade em `/activities/start`.

### Thymeleaf

Thymeleaf renderiza `home.html`, `cylinders.html` e `activity-start.html` no servidor. Os formulários preservam entradas válidas após erros e exibem mensagens escapadas por padrão.

### Tomcat embarcado

O Tomcat já faz parte da aplicação por meio da dependência `spring-boot-starter-webmvc`. Não é necessário instalar ou configurar um servidor separado.

## Dependências aprovadas

- `spring-boot-starter-webmvc`: fornece Spring MVC, rotas HTTP, JSON e Tomcat embarcado.
- `spring-boot-starter-thymeleaf`: integra o Thymeleaf e resolve templates HTML.
- `spring-boot-starter-validation`: valida os campos obrigatórios dos formulários na fronteira web, sem duplicar regras do domínio.
- `spring-boot-devtools`: facilita o desenvolvimento local, mas não é necessário para o funcionamento do JAR.
- `spring-boot-starter-webmvc-test`: fornece MockMvc, testes MVC focados e o suporte geral de testes do Spring Boot.

## Estrutura principal

```text
src/main/java/        Código Java da aplicação
src/main/resources/   Configuração e templates
src/test/java/        Testes automatizados
docs/domain/           Requisitos e decisões canônicas do domínio
.mvn/wrapper/         Configuração do Maven Wrapper
target/               Resultado local da compilação, ignorado pelo Git
```

Arquivos importantes:

- `pom.xml`: configuração do Maven e das dependências.
- `mvnw.cmd`: Maven Wrapper para Windows.
- `mvnw`: Maven Wrapper para Debian e outros sistemas compatíveis com POSIX.
- `RefrigerantControlApplication.java`: ponto de entrada da aplicação.
- `dev.sasser.refrigerantcontrol.domain`: fundamentos do domínio implementados em Java puro.
- `dev.sasser.refrigerantcontrol.application`: casos de uso e resultados imutáveis do fluxo normal.
- `dev.sasser.refrigerantcontrol.application.port`: contratos específicos de armazenamento usados pelos casos de uso.
- `dev.sasser.refrigerantcontrol.configuration`: raiz de composição que conecta as portas, os adaptadores, o relógio e os casos de uso ao Spring.
- `dev.sasser.refrigerantcontrol.infrastructure.memory`: adaptadores em memória, não duráveis e independentes de Spring.
- `dev.sasser.refrigerantcontrol.web.cylinder`: controller e formulários do cadastro operacional de cilindros.
- `dev.sasser.refrigerantcontrol.web.activity`: controller e formulário do início de atividade.
- `dev.sasser.refrigerantcontrol.web.support`: conversão decimal compartilhada pelos fluxos web.
- `application.properties`: nome visível da aplicação.
- `home.html`: página inicial renderizada pelo Thymeleaf.
- `cylinders.html`: página de cadastro de cilindro e peso bruto inicial.
- `activity-start.html`: página de início de atividade.
- `static/css/application.css`: estilos compartilhados, responsivos e sem framework externo.
- `AGENTS.md`: regras permanentes para futuras sessões do Codex.

## Preparação no Windows

Confirme o Java, o compilador e o `JAVA_HOME`:

```powershell
$env:JAVA_HOME
java --version
javac --version
Test-Path "$env:JAVA_HOME\bin\java.exe"
Test-Path "$env:JAVA_HOME\bin\javac.exe"
```

O `JAVA_HOME` pode variar entre instalações do Windows. Ele deve apontar para a pasta raiz de uma instalação do JDK 25 que contenha `bin\java.exe` e `bin\javac.exe`. Nesta máquina, por exemplo, o caminho verificado é:

```text
C:\Program Files\Java\jdk-25
```

As duas verificações com `Test-Path` devem retornar `True`.

## Testar e empacotar no Windows

Confira a versão usada pelo Wrapper:

```powershell
.\mvnw.cmd --version
```

Compile, execute todos os testes e gere o JAR:

```powershell
.\mvnw.cmd verify
```

O arquivo gerado será:

```text
target\refrigerant-control-0.0.1-SNAPSHOT.jar
```

## Iniciar a aplicação no Windows

Durante o desenvolvimento, o plugin do Spring Boot pode iniciar a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

Para provar que o pacote é executável sem depender do processo Maven:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\refrigerant-control-0.0.1-SNAPSHOT.jar
```

A aplicação utiliza a porta local 8080. Encerre uma execução interativa com `Ctrl+C`.

## Verificar as rotas no Windows

Com a aplicação em execução, confira a página HTML:

```powershell
$homeResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080/"
$homeResponse.StatusCode
$homeResponse.Headers["Content-Type"]
$homeResponse.Content
```

Abra o fluxo operacional no navegador em:

```text
http://localhost:8080/cylinders
http://localhost:8080/activities/start
```

Os formulários usam requisições `POST`; o navegador é a forma mais simples de verificar a renderização, a validação e os redirecionamentos dessas páginas. `POST /cylinders` cadastra o cilindro, `POST /cylinders/initial-weight` registra seu peso bruto inicial e `POST /activities/start` inicia a atividade.

Confira o JSON de status:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/status" | ConvertTo-Json
```

O status esperado é:

```json
{
  "application": "Controle de Gases Refrigerantes",
  "status": "UP",
  "environment": "local"
}
```

## Uso posterior no Debian

Se o arquivo `mvnw` perder a permissão de execução durante a transferência a partir do Windows, aplique uma vez:

```bash
chmod +x mvnw
```

Depois, use:

```bash
java -version
./mvnw --version
./mvnw verify
./mvnw spring-boot:run
java -jar target/refrigerant-control-0.0.1-SNAPSHOT.jar
curl --fail http://localhost:8080/
curl --fail http://localhost:8080/cylinders
curl --fail http://localhost:8080/activities/start
curl --fail http://localhost:8080/status
```

O arquivo `.gitattributes` mantém `mvnw` com finais de linha LF e `mvnw.cmd` com CRLF. A permissão executável no Debian é uma configuração separada, por isso o fallback com `chmod` pode ser necessário.

## IntelliJ IDEA

Quando o `pom.xml` existe, o IntelliJ normalmente oferece a opção **Load Maven Project**. Também é possível abrir a janela **Maven** e escolher **Reload All Maven Projects**. O projeto deve continuar usando o JDK 25 já configurado. Depois da importação, `RefrigerantControlApplication` pode ser executada como classe principal.

## Solução de problemas

### `JAVA_HOME` incorreto

Confirme que `JAVA_HOME` aponta para o JDK, não somente para um Java Runtime, e que existe um arquivo `bin\java.exe` dentro dele.

### Versão Java incorreta

Compare as saídas de `java -version`, `javac -version` e `.\mvnw.cmd --version`. Todas devem indicar o Java 25 para este projeto.

### Porta 8080 ocupada

Verifique antes de iniciar:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
```

Encerre de forma segura a outra aplicação que você reconhece antes de tentar novamente. Não termine processos desconhecidos.

### Primeiro uso do Maven mais demorado

No primeiro uso, o Wrapper baixa o Maven 3.9.16 e as dependências. As próximas execuções reutilizam os arquivos do cache local.

### Projeto Maven não atualizado no IntelliJ

Use **Reload All Maven Projects** na janela Maven e confira se o JDK do projeto continua definido como 25.

### Aplicação já em execução

Se uma segunda inicialização informar que a porta 8080 está ocupada, volte ao terminal da primeira execução e encerre-a com `Ctrl+C`.

## Limitações atuais

No estado atual do projeto ainda não existem:

- banco de dados;
- autenticação ou usuários;
- persistência durável ou garantia transacional de banco de dados;
- garantia de unicidade global dos lacres entre processos ou instâncias diferentes dos adaptadores em memória;
- dashboard;
- cadastro editável de gases ou lista de cilindros;
- interface de retorno, conclusão ou consumo de atividades;
- histórico ou listagem de atividades;
- conversão de datas para apresentação em `America/Sao_Paulo`;
- ciclo de cilindro vazio, correção, cancelamento ou importação;
- identificação persistente de atividades, relatórios, backup ou exportação;
- implementação completa do Milestone 2B.

## O que este marco ensina

Ao concluir o primeiro marco, o desenvolvedor entende como Java, Maven, Spring Boot, Spring MVC, Thymeleaf e Tomcat trabalham juntos; como testes verificam o contexto, a página e o JSON; como produzir e executar um JAR; e como manter o mesmo projeto compatível com Windows e Debian.

O Milestone 2B.1 acrescenta o aprendizado sobre objetos de valor, identidade de entidades, imutabilidade, `Optional`, igualdade de `BigDecimal`, transições de estado e testes unitários determinísticos sem Spring.

O Milestone 2B.2 demonstra como casos de uso coordenam o domínio, como portas evitam acoplamento a uma tecnologia de persistência, como um `Clock` torna o tempo determinístico e como snapshots impedem que referências mutáveis alterem acidentalmente o estado armazenado.

O Milestone 2C.1 apresenta a raiz de composição, onde as dependências concretas são conectadas. A inversão de dependência mantém os casos de uso ligados às portas, a injeção por construtor entrega essas dependências sem setters, o escopo singleton preserva uma instância por contexto e o `Clock` injetado separa a obtenção do tempo das regras da aplicação.

O Milestone 2C.2 demonstra como formulários de apresentação, Bean Validation, parsing explícito para `BigDecimal`, mensagens de erro e POST/Redirect/GET formam uma fronteira web simples. O controller coordena HTTP e delega as regras aos casos de uso, enquanto o Thymeleaf mantém a saída escapada e uma folha de estilos compartilhada oferece uma interface responsiva sem JavaScript.

O Milestone 2C.3A demonstra como uma informação obrigatória atravessa agregado, caso de uso, resultado e snapshot sem perder seu valor original. Exceções específicas tornam falhas operacionais distinguíveis, e a verificação da atividade pendente permanece dentro da fronteira atômica do adapter em memória.

O Milestone 2C.3B demonstra como iniciar uma atividade por uma página server-rendered com apenas os dados que o operador precisa informar. O fluxo deriva o gás do cilindro, mantém tempo e situação sob responsabilidade da aplicação, compartilha o parser decimal entre controllers, aplica POST/Redirect/GET e apresenta um resumo escapado sem expor campos internos ou antecipar as interfaces de retorno, conclusão, consumo e histórico.

O Milestone 2C.4A demonstra como uma confirmação operacional obrigatória atravessa agregado, caso de uso, resultado imutável e snapshot sem criar uma interface prematuramente. A conclusão continua atômica, falhas tipadas distinguem pesos negativos, retorno maior que saída e consumo zero não confirmado, e o `Clock` permanece a única origem de `completedAt`.
