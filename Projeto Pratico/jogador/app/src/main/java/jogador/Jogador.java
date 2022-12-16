package jogador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import util.Conexao;

public class Jogador implements AutoCloseable {
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

	private Integer tamanMapa;
	private String nome;
	private String posicaoJogador;
	private String posicaoBandeira;;
	private List<String> listaBandeiras = new ArrayList<String>();
    private String mapa[][];

    public Jogador() throws IOException, TimeoutException {
         // Informações sobre a conexão com o sistema de filas
         ConnectionFactory factory = Conexao.getConnectionFactory();

         connection = factory.newConnection();
         channel = connection.createChannel();
    }

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

    public void close() throws IOException {
        connection.close();
    }

	// Classe que verifica se o jogador está mais próximo da bandeira pela linha ou pela coluna
	// e se movimenta em direção a qual dos dois estiver mais distante
	String movimentaJogador (int xJogador, int yJogador, int novoX, int novoY) {

		int x = 0;
		int y = 0;

		if (Math.abs(yJogador - novoY) != 0) {
			x = xJogador;
			if (yJogador < novoY) {
				y = yJogador + 1;
			} else y = yJogador - 1;

		} else if (Math.abs(xJogador - novoX) != 0) {
			y = yJogador;
			if (xJogador < novoX) {
				x = xJogador + 1;
			} else x = xJogador - 1;
		}

		else {
			x = xJogador;
			y = yJogador;
		}

		return x + "," + y;
	}
 
	String algoritmo () {

		// i - coluna
		// j - linha
		// Formato: (coluna,linha)
		for(int i = 0; i < tamanMapa; i++){
			for(int j = 0; j < tamanMapa; j++){
				// Verifica se o jogador está na posição
				if (mapa[i][j].contains(nome + " ")) {
					posicaoJogador = i + "," + j;
				} 
				// Verficia se há uma ou mais bandeiras na posição
				if (mapa[i][j].contains("b ")) {
					for (int k = 0; k < mapa[i][j].length() ; k++){   
						if(mapa[i][j].charAt(k) == 'b'){   
							posicaoBandeira = i + "," + j;
							listaBandeiras.add(posicaoBandeira);
						}
					}
				}
			}
		}

		int xJogador = Integer.parseInt(posicaoJogador.split(",")[0]);
		int yJogador = Integer.parseInt(posicaoJogador.split(",")[1]);

		// Iniciamos a variável com a maior distância possível entre o jogador e a bandeira
		int menorDist = tamanMapa*2;
		int novoX = 0;
		int novoY = 0;

		// Iterando a lista de bandeiras para verificar a mais próxima ao jogador
		for (int i = 0; i < listaBandeiras.size(); i++) {
			String bandeira = listaBandeiras.get(i);

			// Se já houver uma bandeira no local onde o jogador está, ele permanecerá lá para pegá-la
			if (bandeira == posicaoJogador) {
				novoX = xJogador;
				novoY = yJogador;
				break;
			}

			int xBandeira = Integer.parseInt(bandeira.split(",")[0]);
			int yBandeira = Integer.parseInt(bandeira.split(",")[1]);

			// Verifica o número de casas entre o jogador e a bandeira analisada
			int distancia = Math.abs(xJogador - xBandeira) + Math.abs(yJogador - yBandeira);

			// Caso a distância seja menor ou igual à da bandeira anterior, esta atual será a almejada
			if (distancia <= menorDist) {
				menorDist = distancia;
				novoX = xBandeira;
				novoY = yBandeira;
			}
		}

		String mover = movimentaJogador(xJogador, yJogador, novoX, novoY);

		listaBandeiras.clear();
		
		return mover;
	}

	private static void jogar(Jogador jogador) throws IOException, InterruptedException, ExecutionException{
		String resp = "";
		
		// Início do jogo
		// Fica em um loop consultando o mapa e informando a nova posição ao Auditor
		while (true) {
			int intervaloMovim;
			
			if(resp.contains(jogador.nome + " capturou")){
				intervaloMovim = 2000 + (int)(Math.random() * (2000 - 1000));
			} else
				intervaloMovim = 1000 + (int)(Math.random() * (2000 - 1000));

			Thread.sleep(intervaloMovim);

			String req = "mapa " + jogador.nome;

			System.out.println("[.] " + req);
			resp = jogador.call(req);
			System.out.println("[x] " + resp + "\n");

			if(resp.contains("Vencedor")){
				return;
			}

			if(! (resp.equals("Jogo ainda não iniciado!") || resp.contains("capturou") || resp.equals("Tempo de espera inválido!"))){
				// Reconstruindo o mapa no formato de matriz
				String[] linhas = resp.split(";");
				for(int i = 0; i < jogador.tamanMapa; i++){
					String[] colunas = linhas[i].split(",");
					for(int j = 0; j < jogador.tamanMapa; j++){
						jogador.mapa[i][j] = colunas[j];
					}
				}

				String mover = jogador.algoritmo();

				req = "mover " + jogador.nome + " " + mover;
				
				System.out.println("[.] " + req);
				resp = jogador.call(req);
				System.out.println("[x] " + resp + "\n");
			}
		}

	}

    public static void main(String[] argv) {
        try (Jogador jogador = new Jogador()) {
			String req = "nome";
			String resp;
            
			System.out.println("\n\n" + "[.] " + req);
			// Recebe o nome de jogador gerado pelo Auditor
			resp = jogador.call(req);

			if (resp.contains("Partida iniciada")) {
				System.err.println(resp);
				jogador.close();
				return;
			}

			System.out.println("[x] " + resp + "\n");
			jogador.nome = resp;
			
			req = "tamanho";

			System.out.println("[.] " + req);
			// É informado sobre o tamanho do mapa
			resp = jogador.call(req);
			System.out.println("[x] " + resp + "\n");

			jogador.tamanMapa = Integer.parseInt(resp);

			jogador.mapa = new String[jogador.tamanMapa][jogador.tamanMapa];

			// Aguarda 8 segundos, para que todos os jogadores estejam prontos, antes de iniciar a movimentação
			Thread.sleep(8000);

			req = "pronto " + jogador.nome;

			System.out.println("[.] " + req);
            resp = jogador.call(req);
        	System.out.println("[x] " + resp + "\n");

			jogar(jogador);

			return;

        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}