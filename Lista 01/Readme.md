# *Sobre o Projeto:*
Este projeto tem como finalidade, simular uma conexão entre um servidor e um cliente, utilizando a API de sockets. Esta comunicação se da por meio de dois comandos, _**list**_ e _**get**_.

# *Sobre os Comandos:*
- list - Com este comando, o cliente informa que deseja uma lista contendo todos os arquivos que estão armazenados no servidor.
- get - Com este comando, o cliente deseja que o conteúdo presente em um dos arquivos armazenados no servidor, seja copiado para um arquivo informado ou para um arquivo com o mesmo nome do original.

# *Sobre a Forma de Compilação:*
- Servidor: Para compilar o servidor utilizando a shell do seu computador, basta entrar no diretório onde o arquivo **.java** se encontra e digitar "javac Servidor.java" sem as aspas.
- Cliente: A compilação do cliente se faz de forma análoga a do servidor, apenas substituindo a etapa final, por "javac Cliente.java" sem as aspas.

# *Sobre a Execução:*
- Para executar o servidor utilizando a shell do seu computador, basta seguir os mesmos passos da compilação, porém substiuindo o comando por "java Servidor" sem aspas, seguido do diretório onde estarão os arquivos que ele irá prover ao cliente. Ex: _java Servidor /home/usr/servidor_.
- Para executar o cliente utilizando a shell do seu computador, deve-se seguir os mesmos passos de sua compilação, mas substituindo o comando por "java Cliente" sem aspas, seguido do endereço de IP e porta do servidor (fornecidos na inicialização do mesmo), seguido do comando que deseja-se executar.
  - list: _java Cliente 0.0.0.0 1234 list_. 
  - get: _java Cliente 0.0.0.0 1234 get arquivo.txt saida_ ou apenas _java Cliente 0.0.0.0 1234 get arquivo_.
- Observação 1: O servidor **sempre** deve ser inincializado antes do cliente.
- Observação 2: Caso o usuário deseje mudar o formato do arquivo que será recebido pelo cliente, basta colocar o formato desejado no final do nome. Ex: _java Cliente 0.0.0.0 1234 get arquivo.md saida.txt_. Porém, certos tipos de arquivo, ao mudarem sua formatação, apresentam um comportamento imprevisível, então tenha cuidado.

# *Requisitos atendidos:*
- Todos.