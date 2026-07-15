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

## Tecnologias

### Java 25

Java é a linguagem da aplicação. O projeto compila para Java 25 sem recursos de pré-visualização.

### Maven e Maven Wrapper

Maven lê o arquivo `pom.xml`, resolve dependências, compila o código, executa os testes e empacota a aplicação. O Maven Wrapper fixa a versão Maven 3.9.16 para que o projeto use a mesma ferramenta no Windows e no Debian, mesmo sem uma instalação global do Maven.

### Spring Boot

Spring Boot configura e inicia a aplicação Java. O plugin do Spring Boot também transforma o resultado da compilação em um JAR executável.

### Spring MVC

Spring MVC recebe as requisições HTTP. Ele atende a página inicial, o status e o fluxo de cadastro em `/cylinders`.

### Thymeleaf

Thymeleaf renderiza `home.html` e `cylinders.html` no servidor. Os formulários preservam entradas válidas após erros e exibem mensagens escapadas por padrão.

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
- `dev.sasser.refrigerantcontrol.web.cylinder`: controller, formulários e conversão decimal do cadastro operacional de cilindros.
- `application.properties`: nome visível da aplicação.
- `home.html`: página inicial renderizada pelo Thymeleaf.
- `cylinders.html`: página de cadastro de cilindro e peso bruto inicial.
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
```

Os formulários usam requisições `POST`; o navegador é a forma mais simples de verificar a renderização, a validação e os redirecionamentos dessa página.

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
- cadastro editável de gases, lista de cilindros ou interface para atividades;
- acesso HTTP para iniciar atividades, registrar retornos ou consultar histórico;
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
