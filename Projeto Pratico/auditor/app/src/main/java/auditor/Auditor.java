package auditor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import jogador.jogadorBase;
import util.Conexao;


public class Auditor {
    private static final String RPC_QUEUE_NAME = "rpc_queue";
    private int tamanMapa;
    private int quantBandeiras;
    private String[][] mapa;
    private String mensagemFinal = "";
    private boolean mon = false;
    boolean fim = false;
    private HashMap<String, jogadorBase> jogador;    // A chave é a identificação dos jogadores e o valor, é composto pela quantidade de bandeiras capturadas e pela atual posição do jogador

    /**
     * Inicia os atributos da classe.
     * @param tamanMapa Quantidade de casas que o mapa será dividido, em cada eixo.
     * @param quantBandeiras Quantidade de bandeiras que terá no jogo.
     */
    public Auditor(int tamanMapa, int quantBandeiras) {
        this.jogador = new HashMap<>();

        if(tamanMapa <= 0){
            this.tamanMapa = 2;
        } else
            this.tamanMapa = tamanMapa;

        this.mapa = new String[this.tamanMapa][this.tamanMapa];

        // É necessário iniciar o mapa com alguma coisa, antes de poder fazer as operações
        for(int i = 0; i < this.tamanMapa; i++){
            for(int j = 0; j < this.tamanMapa; j++){
                this.mapa[i][j] = " ";
            }
        }

        // Deve-se no mínimo, ter uma bandeira no jogo
        if(quantBandeiras <= 0){
            this.quantBandeiras = 1;
        
        // A quantidade de bandeiras deve ser ímpar, para evitar um possível empate
        } else if(quantBandeiras % 2 == 0){
            this.quantBandeiras = ++quantBandeiras;
        } else
            this.quantBandeiras = quantBandeiras;
    }

    /**
     * Posiciona os jogadores no mapa.
     */
    private void posicionaJogadores(){
        // Itera o atributo "jogador"
        this.jogador.forEach((ident, jogador)->{
            Random pos = new Random();
            
            // Gera um inteiro aleatório entre 0 e o tamanho do mapa
            int x = pos.nextInt(this.tamanMapa);
            int y = pos.nextInt(this.tamanMapa);
            
            if(this.mapa[x][y].equals(" ")){
                this.mapa[x][y] = ident + " ";
            } else
                this.mapa[x][y] += ident + " "; // Mais de um jogador pode ocupar a mesma casa

            jogadorBase jog = jogador;

            jog.setPosicao(x + "," + y);    // Adiciona a posicao deste jogador, no valor da tabela Hash

            this.jogador.replace(ident, jog);
        });
    }

    /**
     * Posiciona as bandeiras no mapa.
     */
    private void posicionaBandeiras(){
        for(int i = 0; i < this.quantBandeiras; i++){
            Random pos = new Random();
            
            // Gera um inteiro aleatório entre 0 e o tamanho do mapa
            int x = pos.nextInt(this.tamanMapa);
            int y = pos.nextInt(this.tamanMapa);

            if(!this.mapa[x][y].equals(" ")){
                // Uma bandeira não deve ser posicionada em uma casa que já possuir um jogador
                if(this.mapa[x][y].contains("j")){
                    i--;

                // Mais de uma bandeira pode ocupar a mesma casa
                } else
                    this.mapa[x][y] += "b ";
            } else
                this.mapa[x][y] = "b ";
        }
    }

    /**
     * Define o vencedor da partida
     */
    private void defineVencedor(){
        int bandCap = 0;
        String vencedor = "";
        this.mensagemFinal = "";

        for(Entry<String, jogadorBase> jog : this.jogador.entrySet()){
            if(jog.getValue().getQuantBandeiras() > bandCap){
                vencedor = jog.getKey();
                bandCap = jog.getValue().getQuantBandeiras();
            }
        }

        for(Entry<String, jogadorBase> jog : this.jogador.entrySet()){
            if(jog.getKey().equals(vencedor)){
                this.mensagemFinal += "Vencedor: " + vencedor + " bandeiras: " + jog.getValue().getQuantBandeiras() + "\n";
            } else
                this.mensagemFinal += jog.getKey() + " bandeiras: " + jog.getValue().getQuantBandeiras() + "\n";
        }
    }

    /**
     * Realiza a movimentação do jogador.
     * @param identificador Idenrificador único do jogador que deseja se mover.
     * @param novaPosic A nova posição desejada.
     * @return Se o movimento é possível ou não.
     */
    private String movimentaJogador(String identificador, String novaPosic, String hAtual){
        if(this.jogador.containsKey(identificador)){

            jogadorBase jog = this.jogador.get(identificador);

            int xAtual = Integer.parseInt(jog.getPosicao().split(",")[0]);
            int yAtual = Integer.parseInt(jog.getPosicao().split(",")[1]);

            int xNovo = Integer.parseInt(novaPosic.split(",")[0]);
            int yNovo = Integer.parseInt(novaPosic.split(",")[1]);

            // Não permite que o jogador se mova para fora do mapa
            if(xNovo < 0 || xNovo > this.tamanMapa || yNovo < 0 || yNovo > tamanMapa){
                return "inválido";
            }
            
            boolean moveuX;
            switch(Math.abs(xNovo - xAtual)){
                // Não houve movimentação no eixo horizontal
                case 0:
                    moveuX = false; 
                    break;
    
                // Jogador se moveu uma casa no eixo horizontal
                case 1:
                    moveuX = true;
                    break;
                
                // Tentativa de se movimentar por mais de uma casa
                default:
                    return "inválido";
            }

            boolean moveuY;
            switch(Math.abs(yNovo - yAtual)){
                // Não houve movimentação no eixo vertival
                case 0:
                    moveuY = false;
                    break;
                
                // Jogador se moveu uma casa no eixo vertical
                case 1:
                    moveuY = true;
                    break;
                
                // Tentativa de se movimentar por mais de uma casa
                default:
                    return "inválido";
            }
            
            // Impede o jogador de se mover na diagonal
            if(!(moveuX == true && moveuY == true)){
                String resposta = "ok";
                String id = identificador + " ";

                this.mapa[xAtual][yAtual] = this.mapa[xAtual][yAtual].replace(id, " ");   // Remove o jogador daquela casa
            
                this.mapa[xNovo][yNovo] = ((this.mapa[xNovo][yNovo].equals(" ")) ? id : this.mapa[xNovo][yNovo] + id); // Coloca o jogador na nova posição

                // Se na nova casa houver uma bandeira
                if(this.mapa[xNovo][yNovo].contains("b ")){
                    int i = this.mapa[xNovo][yNovo].indexOf("b ");
                    
                    // Retira a primeira bandeira que aparecer na string
                    String aux = this.mapa[xNovo][yNovo].substring(0, i);
                    aux += this.mapa[xNovo][yNovo].substring(i + 2, this.mapa[xNovo][yNovo].length());
                    this.mapa[xNovo][yNovo] = aux;

                    jog.setQuantBandeiras(jog.getQuantBandeiras() + 1);
                    this.quantBandeiras--;

                    if(this.quantBandeiras == 0){
                        this.defineVencedor();
                    }

                    resposta = identificador + " capturou uma bandeira em " + xNovo + "," + yNovo;
                }

                jog.setPosicao(xNovo + "," + yNovo);
                jog.setHrReq(hAtual);

                // Atualiza os dados do jogador
                this.jogador.replace(identificador, jog);

                return resposta;
            }
        }

        return "inválido";
    }

    private String converteMatriz(){
        String response = "";

        // Passando a matriz do campo, pro formato de String. ',' significa uma nova coluna, ';' uma nova linha
        for(int i = 0; i < this.tamanMapa; i++){
            for(int j = 0; j < this.tamanMapa - 1; j++){
                response += this.mapa[i][j] + ",";
            }
            
            response += this.mapa[i][this.tamanMapa - 1] + ";";
        }

        return response;
    }

    /**
     * Estabelece a conexão com o Jogador e com o MonitorGUI.
     * @param auditor Objeto da classe Auditor.
     */
    private static void estabeleceConexao(Auditor auditor){
        // Informações sobre a conexão com o sistema de filas
        ConnectionFactory factory = Conexao.getConnectionFactory();

        Connection connection = null;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            Consumer consumer = new DefaultConsumer(channel) {
                int id = 0;
                ArrayList<String> prontos = new ArrayList<>();
                ArrayList<String> encerrados = new ArrayList<>();
                boolean capturou = false;
                String resp = "";

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {    
                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                            .correlationId(properties.getCorrelationId())
                            .build();

                    String response = "";
                    try {
                        String message = new String(body, "UTF-8");

                        String requisicao = "";
                        String identificador = "";
                        String posicao = "";
                        
                        Date horaMinSeg = new Date();
                        String hAtual = new SimpleDateFormat("HH:mm:ss").format(horaMinSeg);
                        String hReq;

                        try{
                            hReq = auditor.jogador.get(identificador).getHrReq();
                        } catch(Exception e){
                            hReq = "";
                        }
                        
                        // Se houver um espaço na mensagem recebida, interpreta-se que depois dele, haverá dados para o caso "pronto" e o "mover".
                        if(message.contains("j")){
                            requisicao = message.split(" ")[0];
                            identificador = message.split(" ")[1];  // Obtém o id único daquele jogador

                            if(message.contains(","))
                                posicao = message.split(" ")[2];    // Obtém as coordenadas X e Y, que o jogador deseja se mover
                        } else if(message.contains("mon")){
                            auditor.mon = true;
                            identificador = message.split(" ")[1];
                            requisicao = message.split(" ")[0];
                        } else{
                            requisicao = message;
                        }

                        // Se for enviado uma requisição antes do tempo mínimo
                        if(!hReq.equals("") && hReq.compareTo(hAtual) < 1){
                            response = "Tempo de espera inválido!";
                         
                        // Envia para todos os jogadores, uma mensagem informando que um jogador capturou uma bandeira
                        } else if(this.capturou && identificador.contains("j") && !this.prontos.contains(identificador)){
                            this.prontos.add(identificador);
                            
                            response = this.resp;

                            if(this.prontos.size() == auditor.jogador.size()){
                                this.prontos = new ArrayList<>();
                                capturou = false;
                            }
                        
                        // Ao terminar o jogo, envia uma mensagem para todos os jogadores, informando o placar
                        } else if(!auditor.mensagemFinal.equals("")){
                            if(identificador.equals("mon")){
                                this.encerrados.add(identificador);

                                response = "Fim-";
                                response += auditor.converteMatriz();
                            } else if(!this.encerrados.contains(identificador)){
                                this.encerrados.add(identificador);

                                response = auditor.mensagemFinal;
                            }
                        } else
                            switch (requisicao) {
                                // Comunicação inicial entre auditor e jogador, que retorna o identificador único daquele jogador
                                case "nome":
                                    // Não permite que outros jogadores entrem na partida, depois que ela se iniciou
                                    if(this.id != -1){
                                        auditor.jogador.put("j" + id, new jogadorBase(new SimpleDateFormat("HH:mm:ss").format(horaMinSeg)));
                                        response = "j" + id;
                                        this.id++;
                                        prontos.add(response);
                                    } else
                                        response = "Partida iniciada!";   
                                    break;

                                // Informa que o jogador está pronto para iniciar a partida
                                case "pronto":
                                    if(identificador != "" && prontos.contains(identificador)){
                                        this.prontos.remove(identificador);
                                        response = "Ok";

                                        int aux = this.prontos.size();
                                        if(aux == 0 && auditor.jogador.size() >= 2){
                                            this.id = -1;
                                            this.prontos = new ArrayList<>();
                                            auditor.posicionaJogadores();
                                            auditor.posicionaBandeiras();
                                        }
                                    } else
                                        response = "Jogador inesistente!";
                                
                                    break;

                                case "mover":
                                    if(this.id != -1){
                                        response = "Jogo ainda não iniciado!";
                                    } else{
                                        response = auditor.movimentaJogador(identificador, posicao, hAtual);

                                        if(response.contains("capturou")){
                                            this.capturou = true;
                                            this.prontos.add(identificador);
                                            this.resp = response;
                                        }
                                    }    
                                    break;

                                case "mapa":
                                    if(this.id != -1){
                                        response = "Jogo ainda não iniciado!";
                                    } else{
                                        response = auditor.converteMatriz();
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

                        if(auditor.mon && this.encerrados.size() == auditor.jogador.size() + 1){
                            auditor.fim = true;
                        } else if(!auditor.mon && this.encerrados.size() == auditor.jogador.size()){
                                auditor.fim = true;
                        }
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

            while (true) {
                try {
                    Thread.sleep(100);

                    if(auditor.fim){
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
