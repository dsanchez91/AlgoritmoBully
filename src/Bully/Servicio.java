package Bully;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.spi.resource.Singleton;

@Path("servicio")
@Singleton
public class Servicio {
	
	
	private int i=0;																	// Variable que almacena el número de procesos que tiene el servicio
	private String Ip,Id;																// Para guardar los datos que recibimos por REST
	private boolean flag=true;															// Bandera que controla los bucles
	ArrayList<Proceso> procesos = new ArrayList();										// Lista dinámica de procesos que contiene los thread que tiene el servicio
	ArrayList<String> listaIPs = new ArrayList();										// Lista dinámica de cadenas que contiene la lista de IPs de los servicios
	ArrayList<ArrayList<Integer>> listaprocs = new ArrayList<ArrayList<Integer>>();		// Lista de Listas enteras que contiene los procesos de cada servicio según su indice de lista
	
	// Inicializamos el cliente para poder hacer las llamadas REST
	private ClientConfig config = new DefaultClientConfig();
    private Client client = Client.create(config);
    private URI uri;
    private WebResource service;
	
	
	
    /******************************************** MÉTODOS ***************************************/
    
    /********************************** INICIALIZACIÓN **********************************/
	// Constructor
    public Servicio () {}
	
	// Crea un nuevo proceso y lo añade al arrayList de procesos pertenecientes al servicio
	@Path("newproceso")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String CrearProceso (@DefaultValue("0") @QueryParam(value="id") int ID,@DefaultValue("1") @QueryParam(value="estado")  int estado, @DefaultValue("0") @QueryParam(value="coord") int coord) {
		
		procesos.add(new Proceso(ID, true, coord, this));
		i++;
		return "Proceso creado correctamente";
	}
	
	// Añade la ip de un servicio al arrayList de direcciones de los servicios
	@Path("asignarip")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String asignarIPdeServicios (@DefaultValue("0") @QueryParam(value="ip") String ipserv) {
		Ip = ipserv;
		listaIPs.add(Ip);
		listaprocs.add(new ArrayList<Integer>());

	    
	    return "El servicio con IP: " + ipserv + " se ha almacenado en la tabla";
	}
	
	// Asigna las ID’s de los procesos de cada servicio al arrayList de arrayList de enteros
	@Path("asignarid")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String asignarIds (@DefaultValue("0") @QueryParam(value="indice") String indice, @DefaultValue("0") @QueryParam(value="id") String ID) {
		Id = ID;
		listaprocs.get(Integer.parseInt(indice)).add(Integer.parseInt(Id));

		return "El proceso " + Id + " se ha asignado a la tabla";
	}
	
	// Establece el número máximo de procesos
	@Path("numtotalprocesos")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String numtotal (@DefaultValue("0") @QueryParam(value="numtotal") String num) {
		
		for( Proceso hp : procesos){
			
			hp.setNumProcT(Integer.parseInt(num));
		}
		
		return "Establecido el número máximo de procesos";
	}
	
	// Inicia los procesos del servicio
	@Path("iniciar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String iniciar(){
		
		int u;
		
		for(int j=0; j<i;j++)
			procesos.get(j).start();
		
		System.out.print("\n Tabla de Servicios/Procesos:\n");
		u=0;
		for(String k: listaIPs) {
			System.out.print("\t"+ u +") " + k + "\n");
			for(Integer h: listaprocs.get(u)) {
				System.out.print("\t" + h + " - ");
			}
			System.out.print("\n");
			u++;
		
		}
		return "Los procesos del servicio se han iniciado";
	}
	
	// Obtiene el coordinador que tiene establecido cada proceso
	@Path("obtenercoord")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String obtcoord (@DefaultValue("0") @QueryParam(value="proceso") Integer ID) {
		
		int pcoord=-1;
		
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == ID) {
					pcoord = hp.getCoordinador();
					break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de obtener coordinador: Nodo caido");
			}
		}
		
		return pcoord+"";
	}
	
	// Obtiene el estado de la elección que tiene cada proceso
	@Path("estadoelección")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String estelec (@DefaultValue("0") @QueryParam(value="proceso") Integer ID) {
		
		int estado=0;
		
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == ID) {
					estado = hp.getEstadoEleccion();
					break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de obtener estado de la elección: Nodo caido");
			}
		}
		
		return estado+"";
	}
	
	
	
	/********************************** SONDEO **********************************/
	
	// Llama al método EjecutarSondearCoordinador del servicio adecuado, que contiene al proceso numASondear para que lo sondé
	public int NotificarSondeo (int numASondear){
		
		int valor=0;
		int u=0;
		flag=true;
		for(String k: listaIPs) {
			
			for(Integer h: listaprocs.get(u)) {
				
				if (h == numASondear) {
					uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
				    service = client.resource(uri);
				    valor = Integer.parseInt( service.path("rest/servicio/sondear").queryParam("numero",""+numASondear).accept(MediaType.TEXT_PLAIN).get(String.class) );
				    flag=false;
				    break;
				}
				
			}
			
			if(flag==false)
				break;
			u++;
		}
		
		return valor;		
	}
	
	// Sondea al proceso numSondeo (coordinador)
	@Path("sondear")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String EjecutarSondearCoordinador (@DefaultValue("0") @QueryParam(value="numero") int numSondeo) {
		
		int valor=-1;
		
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == numSondeo) {
						valor = hp.sondeo();
						break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de sondeo: Nodo ca�do");
			}
		}
		
		
		return valor+"";
	}
	
	
	/********************************** ELECCIÓN **********************************/
	
	// Llama al método EjecutarEleccion del servicio adecuado (que contiene al proceso "proceso") mandado por el proceso number
	public void NotificarEleccion (int proceso, int number) {
		
		int u=0;
		flag=true;
		for(String k: listaIPs) {
			
			for(Integer h: listaprocs.get(u)) {
				
				if (h == proceso) {
					uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
				    service = client.resource(uri);
				    service.path("rest/servicio/eleccion").queryParam("proceso",""+proceso).queryParam("soy",""+number).accept(MediaType.TEXT_PLAIN).get(String.class);
				    flag=false;
				    break;
				}
				
			}
			u++;
			if(flag==false)
				break;
		}
	}

	// Manda elección al proceso "proceso" enviada por number 
	@Path("eleccion")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String EjecutarEleccion (@DefaultValue("0") @QueryParam(value="proceso") int proceso, @DefaultValue("0") @QueryParam(value="soy") int number) {
						
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == proceso) {
					hp.IniciaEleccion(number);
						break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de Elección: Nodo ca�do"+ proceso);
			}
		}
		
		return "Ejecuta eleccion del proceso " + proceso + " (mandado por " + number;
	}
	
	
	/********************************** OK **********************************/
	
	// Llama al método EjecutarOK del servicio adecuado, que contiene al proceso "proceso" para enviarle un OK
	public void NotificarOK (int proceso) {
		
		int u=0;
		flag=true;
		for(String k: listaIPs) {
			
			for(Integer h: listaprocs.get(u)) {
				
				if (h == proceso) {
					uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
				    service = client.resource(uri);
				    service.path("rest/servicio/ok").queryParam("proceso",""+proceso).accept(MediaType.TEXT_PLAIN).get(String.class); 
				    flag=false;
				    break;
				}
				
			}
			u++;
			if(flag==false)
				break;
		}
	}
	
	// Manda un OK al proceso "proceso"
	@Path("ok")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String EjecutarOK (@DefaultValue("0") @QueryParam(value="proceso") int proceso) {			
	
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == proceso) {
					hp.OK(true);
						break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de OK: Nodo caído"+ proceso);
			}
		}
	
	
	return "Ejecuta el ok al proceso" + proceso;
		
	}

	
	/********************************** COORDINADOR **********************************/
	
	// Llama al método EjecutarCoordinador del servicio adecuado (que contiene al proceso "proceso") para que establazca al coordinador "coord"
	public void NotificarCoordinador (int proceso, int coord) {
		
		int u=0;
		flag=true;
		for(String k: listaIPs) {
			
			for(Integer h: listaprocs.get(u)) {
				
				if (h == proceso) {
					uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
				    service = client.resource(uri);
				    service.path("rest/servicio/coordinador").queryParam("proceso",""+proceso).queryParam("coord",""+coord).accept(MediaType.TEXT_PLAIN).get(String.class);
				    flag=false;
				    break;
				}
				
			}
			u++;
			if(flag==false)
				break;
		}
		
	}
	
	// Manda establecer el coordinador "coord" al proceso "proceso"
	@Path("coordinador")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String EjecutarCoordinador (@DefaultValue("0") @QueryParam(value="proceso") int proceso, @DefaultValue("0") @QueryParam(value="coord") int coord) {
			
		for( Proceso hp : procesos){
			
			try{
					
				if(hp.getNumber()  == proceso) {
					hp.Coordinador(coord);
						break;
				}
			}catch(NullPointerException e) {
				System.out.println("Error de Coordinador: Nodo caído"+ proceso);
			}
		}		
		
		return "Mandale al proceso " + proceso + " que el coordinador es " + coord;
	}

	
	
	/********************************** CONTROL DE HILOS **********************************/
	
	// Manda detenerse al proceso "proceso"
	@Path("detener")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String Detener (@DefaultValue("0") @QueryParam(value="proceso") int proceso) {
		
		for( Proceso hp : procesos){
			
			if(hp.getNumber()  == proceso) {
				hp.Detener();
				break;
			}

		}
		return "El proceso" + proceso + "se ha detenido";
	}
	
	// Manda reanudarse al proceso "proceso"
	@Path("reanudar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String Reanudar (@DefaultValue("0") @QueryParam(value="proceso") int proceso) {
		
		for( Proceso hp : procesos){
					
			if(hp.getNumber()  == proceso) {
				hp.Reanudar();
				break;
			}
			
		}
		
		return "El proceso" + proceso + "se ha reanudado";
	}

	// Manda finalizar a todos los procesos"	
	@Path("Salir")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String salir () {
		
		for( Proceso hp : procesos){			
				hp.Salir();
			}
	
		
		return "Los procesos han concluido su ejecuci�n";
	}
}
