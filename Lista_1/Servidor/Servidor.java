import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Servidor {
    public static String listaArquivos(File arquivos){
        String lista = new String();
        for(File arq: arquivos.listFiles()){
            if(! arq.isDirectory()){
                lista += arq.getName() + "\n";
            }
        }

        return lista;
    }

    public static void main(String[] args) {
        if(args.length == 0){
            System.err.println("\nInsira um diretório!\n");     // Impede que se inicie um servidor sem definir o caminho dos arquivos
            return;
        }

        try(ServerSocket socket = new ServerSocket(1234)){
            System.out.println("Aguardando conexão em: " + socket.getInetAddress() + ":" + socket.getLocalPort());

            while(true){
                try(Socket clientSocket = socket.accept()){
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    String req = entrada.readLine();            // Requisição do cliente

                    File arquivos = new File(args[0]);
                    String lista = listaArquivos(arquivos);     // String contendo os arquivos presentes no diretório

                    if(req.equals("list")){
                        DataOutputStream saida = new DataOutputStream(clientSocket.getOutputStream());

                        saida.write((lista).getBytes(StandardCharsets.UTF_8));    // Se o cliente só quiser a lista dos arquivos, o servior encerra a conexão
                    } else if(req.contains("get")) {
                        String[] arq = req.split(",");

                        if(lista.contains(arq[1])){
                            File arquivo = new File(args[0] + "/" + arq[1]);    // args[0] = caminho em que o arquivo se encontra
                                                                                // arq[1] = nome do arquivo desejado
                            
                            BufferedInputStream b = new BufferedInputStream(new FileInputStream(arquivo));
                            OutputStream s = clientSocket.getOutputStream();

                            long tamanhoArq = arquivo.length();
                            byte[] blocos = new byte[(int) tamanhoArq]; // Cria um vetor de bytes com o tamanho do arquivo
                            long enviados = 0;

                            s.write((int)tamanhoArq);   // Informa ao cliente, o tamanho do arquivo que será enviado

                            while(enviados != tamanhoArq){

                                int blocoEnviado = (int) (tamanhoArq - enviados);

                                if(blocoEnviado >= blocos.length){
                                    blocoEnviado = blocos.length;
                                } 

                                int totalLido = b.read(blocos, 0, blocoEnviado);

                                enviados += totalLido;

                                s.write(blocos);
                            }
                        }
                    } else{
                        String erro = "Comando inválido!\n";

                        DataOutputStream saida = new DataOutputStream(clientSocket.getOutputStream());

                        saida.write(erro.getBytes());
                    }
                } catch (Exception e){
                    System.err.println(e);
                }
            }
        } catch (Exception e){
            System.err.println(e);
        }
    }
}