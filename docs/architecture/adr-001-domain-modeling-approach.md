# ADR-001 — Abordagem para modelagem do domínio

## Status

**Aceito.** A abordagem de modelar e revisar o domínio antes da implementação permanece vigente no Milestone 2A.1. Algumas perguntas de negócio foram respondidas; as perguntas restantes não se tornam decisões arquiteturais por estarem abertas.

## Contexto

O projeto substituirá uma planilha de controle de gases refrigerantes. O Milestone 1 comprovou a base técnica, e o Milestone 2A documentou o primeiro modelo candidato.

O Milestone 2A.1 incorpora confirmação do responsável operacional e um resumo de análise externa da planilha. O arquivo da planilha não está no repositório e não foi inspecionado nesta sessão. A evidência confirmou identidade por lacre, cilindros não recarregáveis, associação imutável com refrigerante, pesos brutos em quilogramas, resolução observada, instantes automáticos e ciclo mínimo do cilindro.

Arredondamento, capacidade da balança, tolerâncias, consumo alto, duplicidade, operações excepcionais e políticas de correção ainda dependem de evidência. Mantê-las abertas evita transformar ausência de informação em regra.

O princípio do produto é **“Completo por dentro, simples por fora.”** A consistência interna não deve aumentar desnecessariamente os campos e passos do fluxo diário.

## Decisão

- Documentar vocabulário, escopo, casos de uso, conceitos, estados, regras, validações e perguntas antes de escrever o domínio Java.
- Manter o modelo mínimo e independente de Spring, MVC, Thymeleaf e mecanismos de persistência.
- Considerar somente `RefrigerantGas`, `Cylinder` e `UsageActivity` como entidades candidatas neste momento.
- Considerar `Weight`, `ActivityStatus` e `CylinderStatus` como representações candidatas, não implementações aprovadas.
- Manter regras do domínio fora de controllers e templates.
- Não criar abstrações para conceitos sem comportamento ou ciclo de vida confirmados.
- Registrar decisões confirmadas sem completar lacunas com suposições.
- Adiar persistência, interface e decisões que ainda dependem de evidência.
- Separar integralmente os Milestones 2A e 2A.1 documentais do Milestone 2B de domínio Java puro e testes.

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
- `sealNumber`, associação imutável, pesos brutos, estados e instantes automáticos podem orientar a futura modelagem quando o Milestone 2B for autorizado.
- `ActivityStatus.AWAITING_RETURN_WEIGHT` descreve somente a ausência da pesagem, sem inferir a situação física do serviço.
- O ciclo candidato `CylinderStatus.ACTIVE`/`EMPTY` evita um fluxo de descarte físico desnecessário e distingue indisponibilidade permanente de atividade pendente.
- A resolução de `0,01 kg` não determina a escala visual da interface nem autoriza arredondamento.
- Decisões sem evidência continuam visíveis no registro em vez de serem escondidas como suposições.
- A documentação precisa manter uma fonte canônica por regra e links entre assuntos relacionados.
- Nenhuma escolha de banco, API ou interface é autorizada por este ADR.

## Decisões adiadas

- rejeição ou arredondamento de valores incompatíveis com `0,01 kg` e eventual modo;
- capacidade máxima da balança, tolerâncias e limiares;
- eventos adicionais de peso fora das fontes confirmadas;
- interação entre cilindro vazio e atividade sem retorno;
- datas e instantes fornecidos em importação ou correção histórica;
- correção, cancelamento, exclusão e autoria;
- identidade interna da atividade, duplicidade e formato do local opcional;
- formatos de backup e exportação;
- fontes e manutenção do catálogo técnico.

Esses itens só podem orientar a parte afetada da implementação depois de registrados como respondidos no [registro de decisões](../domain/open-questions.md). O status aceito deste ADR aprova a abordagem de modelagem, não responde automaticamente perguntas de negócio.
