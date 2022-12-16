package monitorGUI;

import edu.princeton.cs.algs4.Draw;
import java.awt.*;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

import util.Conexao;

public class MonitorGUI implements AutoCloseable {
    private Draw desenho;
    private String[][] mapa;
    private int tamanMapa;
    
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

    /**
     * Gera a tela em que será desenhado o campo, baseando-se no tamanho do mapa.
     */
    private void setDesenho() {
        this.desenho = new Draw();

        // Impede que as últimas casas cortem pela metade
        this.desenho.setXscale(0, tamanMapa + .2);
        this.desenho.setYscale(0, tamanMapa + .2);

        this.desenho.enableDoubleBuffering();
        this.desenho.setPenColor(Color.black);

        this.mapa = new String[this.tamanMapa][this.tamanMapa];
    }

    /**
     * Inicia as variáveis necessárias à conexão.
     * @throws IOException
     * @throws TimeoutException
     */
    public MonitorGUI() throws IOException, TimeoutException {
        // Informações sobre a conexão com o sistema de filas
        ConnectionFactory factory = Conexao.getConnectionFactory();

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    /**
     * Desenha o jogador.
     * @param prop Tamanho do jogador.
     * @param x Eixo horizontal em que o jogador será desenhado.
     * @param y Eixo vertical em que o jogador será desenhado.
     * @param quant Quantidade de jogadores naquela casa.
     */
    private void desenhaJogador(double prop, double x, double y, String jogador){
        this.desenho.circle(x, y, prop * .085);                                 // Desenha a cabeça do personagem
        
        this.desenho.line(x, y - prop * .085, x, y - prop * .3);                // Desenha o corpo

        this.desenho.line(x, y - prop * .3, x - prop * .1, y - prop * .4);      // Desenha a perna esquerda
        this.desenho.line(x, y - prop * .3, x + prop * .1, y - prop * .4);      // Desenha a perna direita

        this.desenho.line(x, y - prop * .15, x - prop * .1, y - prop * .25);    // Desenha o braço esquerdo
        this.desenho.line(x, y - prop * .15, x + prop * .1, y - prop * .25);    // Desenha o braço direito

        this.desenho.text(x, y - prop * .5, jogador);                          // Informa quais jogadores estão naquela casa
    }

    /**
     * Desenha a bandeira.
     * @param prop Tamanho da bandeira.
     * @param x Eixo horizontal em que a bandeira será desenhada.
     * @param y Eixo vertical em que a bandeira será desenhada.
     * @param quant Quantidade de bandeiras naquela casa.
     */
    private void desenhaBandeira(double prop, double x, double y, int quant){
        this.desenho.line(x, y, x, y - prop * .3);  // Desenha a estaca da bandeira

        // Desenha o formato da bandeira
        this.desenho.line(x, y, x + prop * .2, y);
        this.desenho.line(x + prop * .2, y, x + prop * .1, y - prop * .075);
        this.desenho.line(x + prop * .1, y - prop * .075, x + prop * .2, y - prop * .15);
        this.desenho.line(x + prop * .2, y - prop * .15, x, y - prop * .15);

        this.desenho.text(x, y + prop * .1, Integer.toString(quant));   // Informa quantas bandeiras há naquela casa
    }

    /**
     * Preenche as casas do jogo.
     */
    private void desenhaJogo(){
        for(int i = 0; i < this.tamanMapa; i++){
            for(int j = 0; j < this.tamanMapa; j++){
                String aux = this.mapa[i][j];

                String jogadores = "";
                int quantBandeiras = 0;

                while(aux.length() != 0){
                    int inic = aux.indexOf(" ");

                    // Verifica se a primeira posição da string aux, é um jogador ou uma bandeira
                    if(aux.substring(0, 1).equals("j")){
                        jogadores = ((jogadores.equals("")) ? aux.substring(0, inic) : jogadores + ";" + aux.substring(0, inic));
                    } else if(aux.substring(0, 1).equals("b")){
                        quantBandeiras++;
                    }

                    aux = aux.substring(inic + 1);
                }
                
                // Se naquela casa tiver jogadores e bandeiras
                if(!jogadores.equals("") && quantBandeiras != 0){
                    desenhaJogador(0.8, i + .35, j + .9, jogadores);
                    desenhaBandeira(1, i + .8, j + .5, quantBandeiras);
                
                // Se só tiver jogadores 
                } else if(!jogadores.equals("")){
                    desenhaJogador(1.3, i + .6, j + .9, jogadores);

                // Se só tiver bandeiras
                } else if(quantBandeiras != 0){
                    desenhaBandeira(1.5, i + .5, j + .7, quantBandeiras);
                }
            }
        }
    }

    /**
     * Desenha as casas do jogo.
     */
    private void desenhaMapa(){
        this.desenho.clear(Color.white);
        
        // Desenha as grades
        for(double i = .1; i <= this.tamanMapa + .1; i++){
            this.desenho.line(i, .1, i, this.tamanMapa + .1);   // Desenhando linhas verticais
            this.desenho.line(.1, i, this.tamanMapa + .1, i);   // Desenhando linhas horizontais
        }

        desenhaJogo();

        this.desenho.show();
    }

    /**
     * Gera a comunicação com o Auditor.
     * @param message Mensagem que será enviada.
     * @return Resposta recebida.
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public String call(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final CompletableFuture<String> response = new CompletableFuture<>();

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.complete(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.get();
        channel.basicCancel(ctag);
        return result;
    }

    /**
     * Encerra a comunicação.
     */
    public void close() throws IOException {
        connection.close();
    }

    /**
     * Converte o mapa para o formato de uma matriz de Strings.
     * @param mapaString Mapa do jogo no formato de uma String.
     */
    private void converteMapa(String mapaString){
        String[] linhas = mapaString.split(";");

        for(int i = 0; i < this.tamanMapa; i++){
            String[] colunas = linhas[i].split(",");

            for(int j = 0; j < this.tamanMapa; j++){
                this.mapa[i][j] = colunas[j];
            }
        }

        this.desenhaMapa();
    }

    /**
     * Obtém o mapa do jogo no formato de uma String.
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void obtemMapa() throws IOException, InterruptedException, ExecutionException {
        while(true){
            Thread.sleep(1000);

            String resposta = call("mapa mon");

            if(!resposta.equals("Jogo ainda não iniciado!")){
                if(resposta.contains("Fim")){
                    this.converteMapa(resposta.split("-")[1]);
                    return;
                } else
                    this.converteMapa(resposta);
            }
        }
    }

    public static void main(String[] args) {        
        try (MonitorGUI monitor = new MonitorGUI()) {
            String resposta = monitor.call("tamanho mon");

            monitor.tamanMapa = Integer.parseInt(resposta);

            monitor.setDesenho();

            monitor.obtemMapa();

        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}