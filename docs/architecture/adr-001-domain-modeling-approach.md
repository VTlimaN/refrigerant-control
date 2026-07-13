# ADR-001 — Abordagem para modelagem do domínio

## Status

Aceito para o Milestone 2A. As decisões de negócio listadas em [Perguntas abertas](../domain/open-questions.md) permanecem sem resolução individual.

## Contexto

O projeto substituirá uma planilha de controle de gases refrigerantes. O Milestone 1 comprovou a base técnica, mas ainda não existe conhecimento operacional suficiente para implementar regras de domínio com segurança.

Unidade e precisão dos pesos, identidade dos cilindros, mudanças de refrigerante, eventos de peso fora do consumo, datas e políticas de correção ainda dependem de evidência. Implementar antes dessas respostas poderia transformar suposições em regras difíceis de corrigir.

O princípio do produto é **“Completo por dentro, simples por fora.”** A consistência interna não deve aumentar desnecessariamente os campos e passos do fluxo diário.

## Decisão

- Documentar vocabulário, escopo, casos de uso, conceitos, estados, regras, validações e perguntas antes de escrever o domínio Java.
- Manter o modelo mínimo e independente de Spring, MVC, Thymeleaf e mecanismos de persistência.
- Considerar somente `RefrigerantGas`, `Cylinder` e `UsageActivity` como entidades candidatas neste momento.
- Considerar `Weight` e `ActivityStatus` como representações candidatas, não implementações aprovadas.
- Manter regras do domínio fora de controllers e templates.
- Não criar abstrações para conceitos sem comportamento ou ciclo de vida confirmados.
- Adiar persistência e decisões que dependam da planilha ou de validação humana.
- Separar integralmente o Milestone 2A documental do Milestone 2B de domínio Java puro e testes.

## Alternativas consideradas

### Implementar classes durante a descoberta

Rejeitada porque misturaria requisitos ainda abertos com decisões de código e violaria o limite do Milestone 2A.

### Projetar persistência antes do domínio

Rejeitada porque tabelas e ferramentas poderiam determinar o modelo antes de compreender estados, invariantes e histórico.

### Criar um modelo amplo para necessidades futuras

Rejeitada porque aumentaria abstrações, campos e conceitos sem evidência operacional, contrariando a simplicidade externa.

### Manter todas as regras na camada web

Rejeitada porque outros fluxos poderiam contornar invariantes e produzir dados inconsistentes.

## Consequências

- O Milestone 2B terá uma base revisável e vocabulário consistente.
- Decisões sem evidência ficam visíveis em vez de serem escondidas como suposições.
- Alguns detalhes do modelo Java não poderão ser finalizados até a revisão da planilha.
- A documentação precisa manter uma fonte canônica por regra e links entre assuntos relacionados.
- Nenhuma escolha de banco, API ou interface é autorizada por este ADR.

## Decisões adiadas

- unidade, precisão, escala, regras de arredondamento, tara, tolerâncias e limiares;
- identidade do cilindro e conteúdo mínimo dos cadastros;
- atribuição histórica do refrigerante;
- peso inicial e eventos de peso fora do consumo;
- limite de atividades abertas por cilindro;
- datas de saída e retorno, fuso e validações temporais;
- correção, cancelamento, exclusão e autoria;
- formatos de backup e exportação;
- fontes e manutenção do catálogo técnico.

Esses itens só podem orientar implementação depois de registrados como respondidos no [registro de decisões](../domain/open-questions.md).
