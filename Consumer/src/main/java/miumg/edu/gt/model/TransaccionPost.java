package miumg.edu.gt.model;

public class TransaccionPost {

	private String idTransaccion;
    private double monto;
    private String moneda;
    private String cuentaOrigen;
    private String bancoDestino;
    private Detalle detalle;
    private String nombre;
    private String carnet;

    // Constructor
    public TransaccionPost(Transaccion t, String nombre, String carnet) {
        this.idTransaccion = t.getIdTransaccion();
        this.monto = t.getMonto();
        this.moneda = t.getMoneda();
        this.cuentaOrigen = t.getCuentaOrigen();
        this.bancoDestino = t.getBancoDestino();
        this.detalle = t.getDetalle();
        this.nombre = nombre;
        this.carnet = carnet;
    }

    public String getIdTransaccion() { return idTransaccion; }
    public double getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public String getCuentaOrigen() { return cuentaOrigen; }
    public String getBancoDestino() { return bancoDestino; }
    public Detalle getDetalle() { return detalle; }
    public String getNombre() { return nombre; }
    public String getCarnet() { return carnet; }

}