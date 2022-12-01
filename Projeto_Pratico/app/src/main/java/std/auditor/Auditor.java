package std.auditor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import std.conexao.Conexao;


public class Auditor {
    private static final String RPC_QUEUE_NAME = "rpc_queue";
    private int tamanMapa;
    private int quantBandeiras;
    private String[][] mapa;
    private HashMap<String, String> jogador;    // A chave é a identificação dos jogadores e o valor, é composto pela quantidade de bandeiras capturadas e pela atual posição do jogador

    public Auditor(int tamanMapa, int quantBandeiras) {
        jogador = new HashMap<>();

        if(tamanMapa <= 0){
            this.tamanMapa = 2;
        } else
            this.tamanMapa = tamanMapa;

        this.mapa = new String[this.tamanMapa][this.tamanMapa];

        for(int i = 0; i < this.tamanMapa; i++){
            for(int j = 0; j < this.tamanMapa; j++){
                this.mapa[i][j] = "";
            }
        }

        if(quantBandeiras <= 0){
            this.quantBandeiras = 1;
        } else if(quantBandeiras % 2 == 0){
            this.quantBandeiras++;
        } else
            this.quantBandeiras = quantBandeiras;
    }

    private void posicionaJogadores(){
        // Itera o atributo "jogador"
        this.jogador.forEach((ident, bandPos)->{
            Random pos = new Random();
            
            // Gera um inteiro aleatório entre 0 e o tamanho do mapa
            int x = pos.nextInt(this.tamanMapa + 1);
            int y = pos.nextInt(this.tamanMapa + 1);
            
            if(this.mapa[x][y].isEmpty()){
                this.mapa[x][y] = ident + " ";
            } else
                this.mapa[x][y] += ident + " "; // Mais de um jogador pode ocupar a mesma casa

            bandPos += ";" + x + "," + y;   // Adiciona a posicao deste jogador, no valor da tabela Hash
        });
    }

    private void posicionaBandeiras(){
        for(int i = 0; i < this.quantBandeiras; i++){
            Random pos = new Random();
            
            // Gera um inteiro aleatório entre 0 e o tamanho do mapa
            int x = pos.nextInt(this.tamanMapa);
            int y = pos.nextInt(this.tamanMapa);

            // Uma bandeira não deve ser posicionada em uma casa que já possuir um jogador
            if(!this.mapa[x][y].isEmpty()){
                if(this.mapa[x][y].contains("j")){
                    i--;

                // Mais de uma bandeira pode ocupar a mesma casa

                // Mais de uma bandeira pode ocupar a mesma casa
                } else
                    this.mapa[x][y] += "b ";
                    this.mapa[x][y] += "b ";
            } else
                this.mapa[x][y] = "b ";
        }
    }

    private boolean[] verificaMovimento(int novo, int atual){
        switch(Math.abs(novo - atual)){
            // Não houve movimentação
            case 0:
            // O primeiro boolean indica se o jogador vai ou não se mover naquele sentido, o segundo informa se ele está tentando andar mais de uma casa
                return new boolean[] {false, false};
            
            // Jogador se moveu uma casa
            case 1:
                return new boolean[] {true, false};
            
            // Tentativa de se movimentar por mais de uma casa
            // Tentativa de se movimentar por mais de uma casa
            default:
                return new boolean[] {false, true};
        }

    }

    private String movimentaJogador(String identificador, String novaPosic){
        if(this.jogador.containsKey(identificador)){
            String[] valores = this.jogador.get(identificador).toString().split(";");

            int quantBandeiras = Integer.parseInt(valores[0]);  // Obtém a quantidade de bandeiras adquiridas por aquele jogador

            int xAtual = Integer.parseInt(valores[1].split(",")[0]);
            int yAtual = Integer.parseInt(valores[1].split(",")[1]);

            int xNovo = Integer.parseInt(novaPosic.split(",")[0]);
            int yNovo = Integer.parseInt(novaPosic.split(",")[1]);

            // Não permite que o jogador se mova para fora do mapa
            if(xNovo < 0 || xNovo > this.tamanMapa || yNovo < 0 || yNovo > tamanMapa){
                return "false";
            }
            
            boolean[] horizontal = verificaMovimento(xNovo, xAtual);
            boolean[] vertical = verificaMovimento(yNovo, yAtual);

            // Se o jogador tentar se mover por mais de uma casa, em qualquer direção
            if(horizontal[1] || vertical[1]){
                return "false";
            }
            
            // Verifica se o jogador está tentando se mover na diagonal
            if(horizontal[0] != vertical[0]){
                this.mapa[xAtual][yAtual] = this.mapa[xAtual][yAtual].replace(identificador + " " , "");   // Remove o jogador daquela casa
            
                this.mapa[xNovo][yNovo] += identificador + " ";   // Coloca o jogador na nova posição

                // Se na nova casa houver uma bandeira
                if(this.mapa[xNovo][yNovo].contains("b ")){
                    int i = this.mapa[xNovo][yNovo].indexOf("b ");
                    
                    // Retira a primeira bandeira que aparecer na string
                    String aux = this.mapa[xNovo][yNovo].substring(0, i);
                    aux += this.mapa[xNovo][yNovo].substring(i + 2, this.mapa[xNovo][yNovo].length() - 1);
                    this.mapa[xNovo][yNovo] = aux;

                    quantBandeiras++;
                    this.quantBandeiras--;

                    // Incrementa a quantidade de bandeiras capturadas e altera a posição atual do jogador
                    aux = Integer.toString(quantBandeiras) + ";" + xNovo + "," + yNovo;
                    this.jogador.replace(identificador, aux);
                }

                return "true";
            }
        }

        return "false";
    }

    private static void estabeleceConexao(Auditor auditor){
        // Informações sobre a conexão com o sistema de filas
        ConnectionFactory factory = Conexao.getConnectionFactory();

        Connection connection = null;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

            channel.basicQos(1);

            Consumer consumer = new DefaultConsumer(channel) {
                int id = 0;
                int prontos = 0;

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {    
                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                            .correlationId(properties.getCorrelationId())
                            .build();

                    String response = "";

                    try {
                        String message = new String(body, "UTF-8");

                        System.out.println("Recebi: " + message);
                        String[] comando = message.split(" ");

                        switch (comando[0]) {
                            // Comunicação inicial entre auditor e jogador, que retorna o nome único daquele jogador
                            case "nome":
                                // Não permite que outros jogadores entrem na partida, depois que ela se iniciou
                                if(id != -1){
                                    auditor.jogador.put("j" + id, "0");
                                    response = "j" + id;
                                    id++;
                                } else
                                    response = "Partida iniciada!";   
                                break;

                            // Se todos os jogadores estiverem prontos, inicia o jogo
                            case "pronto":
                                prontos++;

                                if(prontos == auditor.jogador.size()){
                                    id = -1;
                                    
                                    auditor.posicionaJogadores();
                                    auditor.posicionaBandeiras();
                                }
                                break;
                            
                            // Se este for o caso, o vetor comando[1] será composto por: jN;x,y
                            case "mover":
                                String identificador = comando[1].split(";")[0];    // Obtém o id único daquele jogador
                                String posicao = comando[1].split(";")[1];          // Obtém as coordenadas X e Y, que o jogador deseja se mover

                                response = auditor.movimentaJogador(identificador, posicao);
                                break;

                            case "mapa":
                                // Passando a matriz do campo, pro formato de String. ',' significa uma nova coluna, ';' uma nova linha
                                for(int i = 0; i < auditor.tamanMapa; i++){
                                    for(int j = 0; j < auditor.tamanMapa - 1; j++){
                                        response += auditor.mapa[i][j] + ",";
                                    }
                                    
                                    // A última linha não terá ';'
                                    if(i == auditor.tamanMapa - 1){
                                        response += auditor.mapa[i][auditor.tamanMapa - 1];
                                    } else
                                        response += auditor.mapa[i][auditor.tamanMapa - 1] + ";";
                                    
                                }
                                break;

                            case "tamanho":
                                response = Integer.toString(auditor.tamanMapa);
                                break;
                        
                            default:
                                response = "Opção Inválida";    
                                break;
                        }
                    } catch (RuntimeException e) {
                        System.out.println(" [.] " + e.toString());
                    } finally {
                        channel.basicPublish("", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));

                        channel.basicAck(envelope.getDeliveryTag(), false);

                        System.out.println("Enviei: " + response);
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

            //loop to prevent reaching finally block
            while (true) {
                try {
                    Thread.sleep(100);
                    
                     // Quando acabar a quantidade de bandeiras, o jogo se encerra
                    if(auditor.quantBandeiras == 0){
                        break;
                    }
                } catch (InterruptedException _ignore) {
                }
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException _ignore) {}
            }
        }
    }

    public static void main(String[] args) {
        if(args.length != 2){
            System.err.println("Quantidade de argumentos inváldia!");
            return;
        }

        int tamMapa;
        int quantBandeiras;
        
        try {
            tamMapa = Integer.parseInt(args[0]);
            quantBandeiras = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("O tamanho do mapa e a quantidade de bandeiras, devem ser valores inteiros!");
            return;
        }

        Auditor a = new Auditor(tamMapa, quantBandeiras);

        estabeleceConexao(a);
    }
}