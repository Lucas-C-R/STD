import java.io.*;
import java.net.Socket;

public class Cliente {
    public static void main(String[] args) {
        String servidorIP = args[0];
        int servidorPorta = Integer.parseInt(args[1]);

        try(Socket conexao = new Socket(servidorIP, servidorPorta)){
            DataInputStream entrada = new DataInputStream(conexao.getInputStream());
            DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());

            String mensagem;

            switch (args[2]){
                case "list":
                    mensagem = args[2] + "\n";  // Envia apenas o comando "list"

                    saida.write(mensagem.getBytes());       // Envia a requisição para o servidor
                    saida.flush();

                    byte[] recebido = entrada.readAllBytes();        // Recebe a mensagem na forma de um vetor de bytes
                    String str = new String(recebido, java.nio.charset.StandardCharsets.UTF_8); // Converte o vetor em uma String

                    System.out.println(str);
                    break;

                case "get":
                    try {
                        mensagem = args[2] + "," + args[3] + "\n";  // A mensagem que será enviada para o servidor vai ser composta pelo "get" e pelo arquivo a ser lido

                        saida.write(mensagem.getBytes());       // Envia a requisição para o servidor
                        saida.flush();

                        FileOutputStream f;

                        if(args.length == 5){
                            f = new FileOutputStream("./" + args[4]);   // Se o usuário quiser que os bytes sejam escritos em um arquivo com nome diferente
                        } else{
                            f = new FileOutputStream("./" + args[3]);   // Se não quiser, copia o nome original
                        }

                        BufferedOutputStream b = new BufferedOutputStream(f);
                        InputStream ent = conexao.getInputStream();

                        int bytesRecebidos = 0;

                        int tamanhoArq = entrada.read();    // Obtém o tamanho do arquivo solicitado

                        byte[] blocos = new byte[tamanhoArq];
                        
                        // O loop perdura enquanto houverem blocos de dados a serem transmitidos
                        while((bytesRecebidos = ent.read(blocos)) != -1){
                            b.write(blocos, 0, bytesRecebidos);
                        }

                        b.flush();
                    } catch(Exception e){
                        System.err.println(e);
                        return;
                    }

                    break;

                default:
                    conexao.close();
                    System.err.println("Comando inválido!");
                    return;
            }
        } catch (Exception e){
            System.err.println(e);
        }
    }
}
