package jogador;

public class jogadorBase {
    private int quantBandeiras;
    private String posicao;
    private String hrReq;
    
    public jogadorBase(String hrReq) {
        this. quantBandeiras = 0;
        this.posicao = "0,0";
        this.hrReq = hrReq;
    }

    public int getQuantBandeiras() {
        return quantBandeiras;
    }

    public void setQuantBandeiras(int quantBandeiras) {
        this.quantBandeiras = quantBandeiras;
    }

    public String getPosicao() {
        return posicao;
    }

    public void setPosicao(String posicao) {
        this.posicao = posicao;
    }

    public String getHrReq() {
        return hrReq;
    }
    
    public void setHrReq(String hrReq) {
        this.hrReq = hrReq;
    }
}
