# Escopo do MVP

O produto segue o princípio **“Completo por dentro, simples por fora.”** O fluxo diário deve permanecer curto, enquanto validação, consistência e rastreabilidade protegem os dados internamente.

Este documento classifica capacidades do produto. O Milestone 2A apenas documenta o domínio; não implementa os itens classificados como MVP.

## Fluxo operacional principal

O formulário normal terá no máximo cinco campos obrigatórios visíveis:

1. cilindro ou seleção operacional equivalente ainda a confirmar;
2. peso de saída;
3. peso de retorno, obrigatório somente para salvar diretamente como concluída;
4. local da atividade;
5. data da atividade.

A data será sugerida como a data corrente e continuará editável. O peso de retorno não será exigido ao registrar uma saída que ficará aguardando retorno. Informações opcionais não ocuparão espaço permanente no fluxo principal.

## Classificação por área

| Área | MVP do produto | Futuro | Não justificado sem requisito confirmado |
|---|---|---|---|
| Atividade operacional | Registrar saída, registrar retorno, concluir atividade, calcular consumo, informar local e data, listar atividades abertas e consultar histórico | Conveniências baseadas em frequência de uso depois que houver dados confiáveis | Dois conceitos ou fluxos independentes para atividade aberta e concluída |
| Cilindro | Cadastro mínimo, identificação, associação com refrigerante e consulta do último peso conhecido | Tratamento próprio para eventos de peso não relacionados a consumo, se a operação confirmar sua existência | Estado de disponibilidade duplicado quando puder ser derivado das atividades |
| Gás refrigerante | Cadastro mínimo que permita identificar o refrigerante | Catálogo técnico enriquecido com composição, segurança, cuidados, referências e manutenção de fontes | Solicitar informações técnicas durante o registro operacional |
| Histórico | Pesquisa e consulta, identificação de registros abertos, correção e cancelamento segundo política aprovada | Auditoria com autoria após existir uma necessidade de identidade de usuário | Exclusão física como operação normal |
| Proteção de dados | Backup e exportação conforme formatos ainda a confirmar | Importação e rotinas administrativas ampliadas | Escolher formatos ou infraestrutura sem requisito operacional |
| Automação | Data corrente, cálculo de consumo, sugestão segura do último peso conhecido, preservação dos dados após erro e operação eficiente por teclado | Locais recentes, lembrança da última seleção e ordenação por frequência | Decisões automáticas irreversíveis |

## Separação dos dados

### Dados da atividade operacional

- cilindro selecionado;
- peso de saída;
- peso de retorno, quando disponível;
- local;
- data ou datas operacionais conforme decisão pendente;
- estado da atividade;
- confirmações excepcionais e rastreabilidade interna quando aplicáveis.

### Dados do cilindro

- identidade operacional ainda a confirmar;
- associação com gás refrigerante;
- situação histórica da associação, ainda em decisão;
- informações necessárias para estabelecer o último peso conhecido, caso confirmadas pela operação.

### Dados do catálogo de refrigerantes

- nome padronizado;
- descrição e aplicações;
- composição e classe de segurança;
- características, cuidados e armazenamento;
- referências técnicas confiáveis.

Somente a identificação mínima do gás pertence ao MVP inicial. Os dados técnicos ficam fora do formulário operacional.

### Funções administrativas

Backup, exportação, eventual importação, usuários e configurações pertencem a uma área secundária. Backup e exportação fazem parte do MVP, mas seus formatos permanecem em aberto. Autenticação, autorização e importação não pertencem ao Milestone 2A nem ao Milestone 2B.

## Informações opcionais

| Informação | Classificação inicial | Motivo |
|---|---|---|
| Ordem de serviço | Futuro | Depende de frequência e necessidade operacional comprovadas. |
| Técnico | Futuro | Não há requisito confirmado nem autenticação para estabelecer autoria. |
| Observações | Futuro | Pode aumentar a entrada de dados sem benefício demonstrado. |
| Horários de saída e retorno | Futuro | Datas e instantes necessários ainda precisam ser confirmados. |
| Motivo de correção | MVP apenas no fluxo excepcional | Não aparece no formulário normal; é solicitado somente quando a política exigir correção ou cancelamento. |
| Detalhes adicionais | Não justificado sem requisito confirmado | A expressão é ampla e não define uma necessidade verificável. |

Um campo verdadeiramente opcional vazio não gera alerta por padrão. Qualquer mudança nessa regra exige evidência operacional e deve ser registrada em [Perguntas abertas](open-questions.md).

## Fora do Milestone 2A

O marco não cria classes, telas, navegação, APIs, persistência, segurança, backup ou exportação. O limite entre a documentação atual e a futura implementação Java está registrado no [ADR de abordagem da modelagem](../architecture/adr-001-domain-modeling-approach.md).

