# Projeto Prático - Jogo de captura de bandeiras
### Alunos: Lucas Coelho Raupp e Yago Castro Rosa

>O software desenvolvido permite que um processo "Auditor" se conecte com N processos "Jogador" para o estabelecimento de um jogo de captura à bandeira. Além disso, existe um processo "Monitor" que mostra o andamento do jogo em tempo real por meio de uma interface gráfica. Este laboratório foi desenvolvido utilizando a linguagem Java para o Auditor, Jogador e Monitor, sendo que a comunicação entre eles foi implementada por meio de uma fila de mensagens (RabbitMQ). Não há interação do usuário com o jogo, uma vez que após todas as partes estarem ligadas e se comunicando o algoritmo de menor caminho faz com que o "Jogador" priorize a captura da bandeira mais próxima e o movimente automaticamente.

## Tarefas feitas pelo Lucas
- Classes "Auditor" e "Monitor".
- Documentação das classes que ficou encarregado.
- Configuração do RabbitMQ em todas as classes do projeto.
- .gitignote, launch.json e docker.


## Tarefas feitas pelo Yago
- Classe "Jogador" e a documentação da mesma.
- Arquivo Readme.md.

## O que foi feito
- Comunicação entre "Auditor", "Jogador" e "Monitor" utilizando fila de mensagens.
- "N" jogadores podem participar do jogo e o auditor consegue gerenciar as diferentes requisições feitas por cada um deles.
- Cada processo pode ser executado de forma individual.

## O que não pôde ser feito
- Foi sugerido que utilizássemos o "algoritmo de menor caminho", porém encontramos dificuldades em adaptá-lo para as necessidades do nosso projeto. Desta forma, desenvolvemos uma solução própria que verifica qual bandeira está mais próxima do jogador e este se movimenta primeiro na vertical e depois na horizontal até alcançá-la. Sendo assim, a solução foi entregue, porém não seguindo a sugestão dada pelo professor.

## Manual de utilização
1. Com o terminal aberto dentro do diretório do projeto, digite: "docker compose build". Dependendo das permissões do seu usuário, pode ser necessário executar o comando "sudo docker compose build". Esta especificidade se aplica para os demais passos deste manual.
2. No mesmo terminal utilizado no ítem anterior, digite "docker compose up rabbit". Aguarde o processo subir e não feche o terminal até o final do jogo.
3. Em outro terminal, porém no mesmo diretório raiz do projeto, digite "docker compose up auditor" e aguarde até que o processo tenha subido por completo. Não feche o terminal até o final do jogo.
4. A partir de agora você pode subir o processo "monitorGUI" para acompanhar o desdobramento da partida, porém você também pode executá-lo após o início da partida com a chegada dos jogadores (após o ítem 5). Com um novo terminal aberto dentro do diretório "monitorGUI" do projeto, digite "./gradlew run" e aguarde o monitor ser apresentado à você.
5. Em outro terminal, porém no diretório raiz do projeto, digite "docker compose up --scale jogador=N", sendo "N" o número de jogadores que serão inseridos na partida, e aguarde até que o processo tenha subido por completo (utilize um valor inteiro). Não feche o terminal até o final do jogo e acompanhe tanto as movimentações dos jogadores quanto o placar por meio do terminal.
6. Ao finalizar o jogo, digite "docker compose down" em outro terminal para encerrar os processos em execução.
>Observação: a partida é predefinida para possuir 25 bandeiras e um mapa com proporções 8x8, entretanto você pode mudar estes valores dentro do arquivo "docker-compose.yml" alterando os parâmetros "BANDEIRAS" e "TAMANHO", respectivamente. Lembre-se de utilizar apenas valores inteiros.