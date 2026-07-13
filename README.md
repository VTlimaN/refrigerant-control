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

Este marco não contém banco de dados, autenticação, regras de negócio, painel, cadastro de gases, cadastro de cilindros ou outras telas operacionais.

## Tecnologias

### Java 25

Java é a linguagem da aplicação. O projeto compila para Java 25 sem recursos de pré-visualização.

### Maven e Maven Wrapper

Maven lê o arquivo `pom.xml`, resolve dependências, compila o código, executa os testes e empacota a aplicação. O Maven Wrapper fixa a versão Maven 3.9.16 para que o projeto use a mesma ferramenta no Windows e no Debian, mesmo sem uma instalação global do Maven.

### Spring Boot

Spring Boot configura e inicia a aplicação Java. O plugin do Spring Boot também transforma o resultado da compilação em um JAR executável.

### Spring MVC

Spring MVC recebe as requisições HTTP. Neste marco, ele atende apenas `GET /` e `GET /status`.

### Thymeleaf

Thymeleaf renderiza o arquivo `home.html` no servidor e produz a página HTML inicial.

### Tomcat embarcado

O Tomcat já faz parte da aplicação por meio da dependência `spring-boot-starter-webmvc`. Não é necessário instalar ou configurar um servidor separado.

## Dependências aprovadas

- `spring-boot-starter-webmvc`: fornece Spring MVC, rotas HTTP, JSON e Tomcat embarcado.
- `spring-boot-starter-thymeleaf`: integra o Thymeleaf e resolve templates HTML.
- `spring-boot-starter-validation`: disponibiliza Bean Validation para etapas futuras, sem regras de domínio neste marco.
- `spring-boot-devtools`: facilita o desenvolvimento local, mas não é necessário para o funcionamento do JAR.
- `spring-boot-starter-webmvc-test`: fornece MockMvc, testes MVC focados e o suporte geral de testes do Spring Boot.

## Estrutura principal

```text
src/main/java/        Código Java da aplicação
src/main/resources/   Configuração e templates
src/test/java/        Testes automatizados
.mvn/wrapper/         Configuração do Maven Wrapper
target/               Resultado local da compilação, ignorado pelo Git
```

Arquivos importantes:

- `pom.xml`: configuração do Maven e das dependências.
- `mvnw.cmd`: Maven Wrapper para Windows.
- `mvnw`: Maven Wrapper para Debian e outros sistemas compatíveis com POSIX.
- `RefrigerantControlApplication.java`: ponto de entrada da aplicação.
- `application.properties`: nome visível da aplicação.
- `home.html`: página inicial renderizada pelo Thymeleaf.
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

Neste primeiro marco ainda não existem:

- banco de dados;
- autenticação ou usuários;
- regras de consumo de gás;
- dashboard;
- cadastro de gases;
- cadastro de cilindros;
- atividades, histórico, relatórios, backup ou exportação.

## O que este marco ensina

Ao concluir este marco, o desenvolvedor entende como Java, Maven, Spring Boot, Spring MVC, Thymeleaf e Tomcat trabalham juntos; como testes verificam o contexto, a página e o JSON; como produzir e executar um JAR; e como manter o mesmo projeto compatível com Windows e Debian.
