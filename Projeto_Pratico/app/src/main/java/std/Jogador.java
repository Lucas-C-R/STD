// package std.jogador;

// import org.zeromq.SocketType;
// import org.zeromq.ZContext;
// import org.zeromq.ZMQ;

// public class Jogador {
//     private String identificador;
//     private int[][] mapa;
//     public static void main(String[] args) {

//         if (args.length != 1) {
//             System.err.println("Quantidade de argumentos inváldia!");
//         }

//         try (ZContext context = new ZContext()) {
//             // Socket para conversar com o servidor
//             ZMQ.Socket socket = context.createSocket(SocketType.REQ);

//             System.out.println("Conectando no servidor Hello World!");
//             // Fica bloqueado até conseguir se conectar no servidor
//             // args[0] informa endereçoDeIP:Porta
//             socket.connect("tcp://" + args[0]);

//             System.out.println("Enviando mensagem ");
//             String request = "ola";
//             // Envia a mensagem (sequência de bytes) para estabelecer a comunicação
//             socket.send(request.getBytes(ZMQ.CHARSET), 0);

//             // Fica bloqueado até receber a resposta do servidor
//             byte[] reply = socket.recv(0);
//             System.out.println("Recebido " + new String(reply, ZMQ.CHARSET));


//             // Serão informada a dimensão do mapa, a posição do jogador e as posições das bandeiras (por meio de um arquivo?)
//             // Utilizará o algoritmo para calcular a distância entre o ponto atual e as bandeiras posicionadas no mapa
//             // Informará ao servidor qual a nova posição de interesse para posicionar-se
//             // Recebe as informações do servidor na nova rodada e calcula novamente
//             int grafo[][] = new int[][] { { 0, 0, 1, 2, 0, 0, 0 }, { 0, 0, 2, 0, 0, 3, 0 }, { 1, 2, 0, 1, 3, 0, 0 },
//             { 2, 0, 1, 0, 0, 0, 1 }, { 0, 0, 3, 0, 0, 2, 0 }, { 0, 3, 0, 0, 2, 0, 1 }, { 0, 0, 0, 1, 0, 1, 0 } };
//         }
//     }
// }
