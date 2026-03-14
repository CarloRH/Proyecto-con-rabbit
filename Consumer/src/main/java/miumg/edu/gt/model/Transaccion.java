package miumg.edu.gt.model;

public class Transaccion {

	private String idTransaccion;
    private double monto;
    private String moneda;
    private String cuentaOrigen;
    private String bancoDestino;
    private Detalle detalle;

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String id) { this.idTransaccion = id; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String c) { this.cuentaOrigen = c; }

    public String getBancoDestino() { return bancoDestino; }
    public void setBancoDestino(String b) { this.bancoDestino = b; }

    public Detalle getDetalle() { return detalle; }
    public void setDetalle(Detalle detalle) { this.detalle = detalle; }

}