package std.monitorGUI;

import edu.princeton.cs.algs4.Draw;
import java.awt.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import std.conexao.Conexao;

public class MonitorGUI {
    private Draw desenho;
    private String[][] mapa;
    private int tamanMapa;
    
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";
    private String replyQueueName;

    private void setDesenho() {
        this.desenho = new Draw();

        // Impede que as últimas casas cortem pela metade
        this.desenho.setXscale(0, tamanMapa + .2);
        this.desenho.setYscale(0, tamanMapa + .2);

        this.desenho.enableDoubleBuffering();
        this.desenho.setPenColor(Color.black);
    }

    public MonitorGUI() throws IOException, TimeoutException {
        // Informações sobre a conexão com o sistema de filas
        ConnectionFactory factory = Conexao.getConnectionFactory();

        connection = factory.newConnection();
        channel = connection.createChannel();

        replyQueueName = channel.queueDeclare().getQueue();
    }

    private void desenhaJogador(double prop, double x, double y, int quant){
        this.desenho.circle(x, y, prop * .085);                                 // Desenha a cabeca do personagem
        
        this.desenho.line(x, y - prop * .085, x, y - prop * .3);                // Desenha o corpo

        this.desenho.line(x, y - prop * .3, x - prop * .1, y - prop * .4);      // Desenha a perna esquerda
        this.desenho.line(x, y - prop * .3, x + prop * .1, y - prop * .4);      // Desenha a perna direita

        this.desenho.line(x, y - prop * .15, x - prop * .1, y - prop * .25);    // Desenha o braço esquerdo
        this.desenho.line(x, y - prop * .15, x + prop * .1, y - prop * .25);    // Desenha o braço direito

        this.desenho.text(x, y - prop * .48, Integer.toString(quant));          // Informa a quantidade de jogadores naquela casa
    }

    private void desenhaBandeira(double prop, double x, double y, int quant){
        this.desenho.line(x, y, x, y - prop * .3);  // Desenha a estaca da bandeira

        // Desenha o formato da bandeira
        this.desenho.line(x, y, x + prop * .2, y);
        this.desenho.line(x + prop * .2, y, x + prop * .1, y - prop * .075);
        this.desenho.line(x + prop * .1, y - prop * .075, x + prop * .2, y - prop * .15);
        this.desenho.line(x + prop * .2, y - prop * .15, x, y - prop * .15);

        this.desenho.text(x, y + prop * .1, Integer.toString(quant));   // Informa quantas bandeiras há naquela casa
    }

    private void desenhaJogo(){
        for(int i = 0; i < this.tamanMapa; i++){
            for(int j = 0; j < this.tamanMapa; j++){
                String aux = this.mapa[i][j];

                int quantJogadores = 0;
                int quantBandeiras = 0;

                while(aux.length() != 0){
                    int inic = aux.indexOf(" ");

                    // Verifica se a primeira posição da string aux, é um jogador ou uma bandeira
                    if(aux.substring(0, 1).equals("j")){
                        quantJogadores++;
                    } else if(aux.substring(0, 1).equals("b")){
                        quantBandeiras++;
                    }

                    aux = aux.substring(inic + 1);
                }
                
                // Se naquela casa tiver um jogador e uma bandeira
                if(quantJogadores != 0 && quantBandeiras != 0){
                    desenhaJogador(1, i + .35, j + .9, quantJogadores);
                    desenhaBandeira(1, i + .8, j + .5, quantBandeiras);
                
                // Se não tiver nenhuma bandeira 
                } else if(quantJogadores != 0){
                    desenhaJogador(1.5, i + .6, j + .9, quantJogadores);

                // Se não tiver nenhum jogador
                } else if(quantBandeiras != 0){
                    desenhaBandeira(1.5, i + .5, j + .7, quantBandeiras);
                }
            }
        }
    }

    private void desenhaMapa(){
        this.desenho.clear(Color.white);
        
        // Desenha o mapa do jogo
        for(double i = .1; i <= this.tamanMapa + .1; i++){
            this.desenho.line(i, .1, i, this.tamanMapa + .1);   // Desenhando linhas verticais
            this.desenho.line(.1, i, this.tamanMapa + .1, i);   // Desenhando linhas horizontais
        }

        desenhaJogo();

        this.desenho.show();
    }

    private String call(String message) throws IOException, InterruptedException {
        String corrId = UUID.randomUUID().toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);

        channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (properties.getCorrelationId().equals(corrId)) {
                    response.offer(new String(body, "UTF-8"));
                }
            }
        });

        return response.take();
    }

    private void close() throws IOException {
        connection.close();
    }

    private void atualizaMapa() throws IOException, InterruptedException{
        while(true){
            System.out.println("Enviei: mapa ");
            String resposta = call("mapa ");

            System.out.println("Recebi: " + resposta);

            String[] linhas = resposta.split(";");

                for(int i = 0; i < this.tamanMapa; i++){
                    String[] colunas = linhas[i].split(",");

                    for(int j = 0; j < this.tamanMapa; j++){
                        this.mapa[i][j] = colunas[j];
                    }
                }

            this.desenhaMapa();
        }
    }

    public static void main(String[] args) {
        MonitorGUI monitor = null;
        String resposta = null;

        try {
            monitor = new MonitorGUI();

            System.out.println("Enviei: tamanho ");

            resposta = monitor.call("tamanho ");

            System.out.println("Recebi: " + resposta);
            
            monitor.tamanMapa = Integer.parseInt(resposta);

            monitor.setDesenho();

            monitor.atualizaMapa();

        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (monitor != null) {
                try {
                    monitor.close();
                } catch (IOException _ignore) {}
            }
        }
    }
}