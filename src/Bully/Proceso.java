package Bully;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class Proceso extends Thread  {
	
	
	private Servicio miServ;									// Instancia de su servicio
	private boolean estado = true;								// Estado del proceso que indica si está en ejecución o parado 	
	private boolean OK = false;									// Indica si el proceso a recibido un OK
	private boolean recibidoCoordinador = false;				// Indica si el proceso a recibido un coordinador
	private boolean acabo = false;								// Indica si el proceso debe acabar su ejecución
	private Semaphore semOK = new Semaphore(0,true);			// Semáforo para controlar la recepción de OK
	private Semaphore semCoord = new Semaphore(0,true);			// Semáforo para controlar la recepción de un coordinador
	private Semaphore apagado = new Semaphore(0,true);			// Semáforo para controlar cuando estamos detenidos
	private int coordinador;									// Variable que guarda el coordinador
	private int number;											// Variable que almacena el ID del proceso
	private int numProcT=0;										// Variable que guarda el número de procesos totales
	private int estadoEleccion=0;   							/* Variable que almacena el estado de la elección:
																			0 = No activa
																			1 = Decidiendo
																			2 = Esperando */
		
	/********************************** METODOS **********************************/
	
	// Constructor vacio
	public Proceso () { }
	
	// Constructor con inicializador de atributos
	public Proceso (int ID, boolean estado, int coordinador, Servicio serv) {
		
		this.number = ID;
		this.estado = estado;
		this.coordinador = coordinador;
		this.miServ = serv;
		this.OK = false;
		this.recibidoCoordinador=false;
		
	}
	

	// Metodo que ejecutará nuestro Thread.
	public void run()
	{	
		System.out.println("Proceso " + number + ": Estoy en marcha. Coordinador es " + coordinador);
		long espera=0;
		
		//Provoca una eleción ya que en este momento no habrá coordinador
		this.eleccion(0);
		
		while(true)
		{
			// Control de ejecución para detener, reanudar o finalizar el hilo	
			// Si tu estado es detenido
			if (estado == false) {
				
				// Si se ha elegido finalizar la ejecución
				if (acabo == true) {
					
					System.out.println("Proceso " + number + ": Concluyo mi ejecución");
					return;
			
				}else{
					// Si no detenemos la ejecución
					System.out.println("Proceso " + number + ": Detengo mi ejecución");
					try {
						apagado.acquire(1);
					}catch(InterruptedException e){}
					
					// Al reanudar ejecución comprobamos si debemos finalizar
					if (acabo == true) {
						
						System.out.println("Proceso " + number + ": Concluyo mi ejecución");
						return;
				
					}else {
						// Si no reanudamos la ejecucióny provocamosuna elección
						System.out.println("Proceso " + number + ": Reanudo mi ejecución");
						this.estado=true;
						this.eleccion(0);
					}
				}

			} else {
				
				// Espero entre 0,5 y 1 segundos
				espera = (long) (Math.random()*1000+500);
				try{			
					Thread.sleep(espera); 								
				}catch(InterruptedException e){}
				
				// Si no eres el coordinador
				if (coordinador != number) {
					
					// Y tu id es menor que la del coordinador
					if(number < coordinador) {
							
							// Sondeamos, si no está activo y nuestro estado elección es de cero provocamos una elección (Si no es cero signifiva que ya hay una activa)
							if(this.miServ.NotificarSondeo(coordinador) < 0) {
								System.out.println("Proceso " + number + ": El coordinador "+ coordinador+" esta caido. (Estado de la eleccion = "+ estadoEleccion);
								if(this.estadoEleccion==0)
									this.eleccion(0);
							}
							else{
								System.out.println("Proceso " + number + ": El coordinador "+ coordinador+" esta en funcionamiento.");
							}
										
					}else {
						// El coordinador es más pequeño que tu ID (debe haber ocurrido un error) por lo que llamamos a elección 
						System.out.println("Proceso " + number + ": El coordinador "+ coordinador+" es menor que yo. (Estado de la eleccion = "+ estadoEleccion);
						if(this.estadoEleccion==0) {
							System.out.println("Proceso " + number + ": Por lo que mando eleccion.");
							this.eleccion(0);
						}
							
					}//else
				}//if
			}//else
		}//while
	}//run
	
	
	
	
	/********************************** ALGORITMO BULLY **********************************/
	
	/* Método del coordinador que retorna un 1, si el coordinador está activo tras esperar
		un tiempo para simular que realiza alguna acción. Y -1 si no está activo el proceso */
	public int sondeo () {
		
		if(this.estado== true) {
			//System.out.println("Proceso " + number + ": Estoy vivo");
			long espera = (long) (Math.random()*300+100);
			try{			
				Thread.sleep(espera); 								
			}catch(InterruptedException e){}
			
			return 1;
		}else
			return -1;
	}
	
	
	// Metodo de elección
	public void eleccion (int nodo) {
		
		int i = this.number;					// Establecemos i al ID del proceso
		int j=0;								// Variable para iterar en bucles
		this.semOK = new Semaphore(0,true);		// Reiniciamos el semaforo de OK para una nueva elección
		this.semCoord = new Semaphore(0,true);	// Reiniciamos el semaforo de Coord para una nueva elección
		
		this.estadoEleccion=1;			// (decidiendo)
		
		//Vemos si la elección a sido provocada por nosotros o un proceso nos ha enviado elección
		if(nodo == 0)
			System.out.println("Proceso " + number + ": Eleccion al darme cuenta de que el coordinador esta caido");
		else
			System.out.println("Proceso " + number + ": Eleccion mandada por " + nodo);
		
		
		// Si somos el proceso con mayor ID 
		if(i == numProcT) {
			
			// Si un proceso nos ha enviado la eleccion enviamos un OK a ese proceso nodo
			if(nodo != 0)
			{
					if(this.number > nodo) { 
							
						this.miServ.NotificarOK(nodo);  //enviar mensaje de OK a NODO
						System.out.println("Proceso " + number + " (eleccion): Mando OK a "+ nodo);
					}
			}
						
			// Nos ponemos como coordinador y se lo notificamos a todos los procesos
			System.out.println("Proceso " + number + ": Notifico que soy el coordinador. Porque soy el proceso con mayor ID");
			this.coordinador = numProcT;
			for(j=1; j <= this.number; j++)
				this.miServ.NotificarCoordinador(j, this.number);
			
			this.estadoEleccion=0;		// restauramosel estado de la elección
			
		} else { 
				
			// Si un proceso nos ha enviado la eleccion enviamos un OK a ese proceso nodo
			if(nodo != 0)
			{
				if(this.number > nodo) {
					
					this.miServ.NotificarOK(nodo);
					System.out.println("Proceso " + number + " (eleccion): Mando OK a "+ nodo);
				}
			}
			
			
			/* Propagamos la eleccion a los procesos con un ID mayor hasta que recibamos un OK o Coordinador
			    o hasta que se lo enviemos a todos los procesos mayores */
		    i++;
			while( i <= numProcT) {
				if(this.OK == false || this.recibidoCoordinador==false) {
					this.miServ.NotificarEleccion(i,this.number);
					System.out.println("Proceso " + number + ": Mando Eleccion a "+ i);				
				}
				i++;
			}
			
			this.estadoEleccion=2;		// (Esperando)
			
			//Esperar mensaje de respuesta
			try{
				System.out.println("Proceso " + number + ": Esperando por un ok");
				semOK.tryAcquire(1, TimeUnit.SECONDS);
				System.out.println("Proceso " + number + ": Salgo de esperar ok");
			}catch(InterruptedException e){}
			
			//Si se recibe mensaje de respuesta (OK)
			if(this.OK == true) {
				System.out.println("Proceso " + number + ": He aceptado un ok");
				 this.OK = false;
			
				 
				//Esperar mensaje de coordinador
					try{			
						System.out.println("Proceso " + number + ": Esperando por un coordinador");
						semCoord.tryAcquire(1, TimeUnit.SECONDS);
						System.out.println("Proceso " + number + ": Salgo de esperar coordinador");
					}catch(InterruptedException e){}
				
				//Si se recibe mensaje de coordinador
				if (this.recibidoCoordinador == true) {
					System.out.println("Proceso " + number + ": He aceptado al nuevo coordinador ok");
					this.recibidoCoordinador = false;
					this.estadoEleccion=0;
					return;
				
				//Si no se recibe el mensaje de coordinador provocamos una nueva elección
				}else{	
					System.out.println("Proceso " + number + ": No ha llegado ningun coordiandor. Comienzo nueva eleccion");
					this.eleccion(0);
				}
				
			//Si no se recibe mensaje de respuesta nos establecemos como coordinador
			}else{
				this.coordinador = this.number;
				System.out.println("Proceso " + number + ": Notifico que soy el coordinador. Porque no ha llegado ningun ok");
				
				for(j=1; j <= numProcT; j++) 
						this.miServ.NotificarCoordinador(j, this.number);
				
				this.estadoEleccion=0;
					
				return;
			}
		}

	}
	
	
	/* Este método es llamado por otros procesos para notificar a este proceso que debe comenzar una
	 * elección. Comprueba si el estado de la elección para ver si debe lanzarse una nueva elección (no
	 * hay otra activa) o simplemente responder con un OK al nodo que nos ha mandado elección (estado
	 * decidiendo o esperando por un OK y coordinador.
	 */
	public void IniciaEleccion (int nodo)
	{
		System.out.println("Proceso " + number + ": Inicia Eleccion por "+ nodo + " y mi estado es de " + this.estadoEleccion);

		if(this.estado== true) {
	
			if (this.estadoEleccion == 0) {
				this.eleccion(nodo);
				
			} else if (this.estadoEleccion == 1 || this.estadoEleccion == 2 ) {
				if(this.number > nodo) { 
					this.miServ.NotificarOK(nodo);  //enviar mensaje de OK a NODO
					System.out.println("Proceso " + number + ": Mando OK a "+ nodo + " (Inicia Eleccion. Estado =" + estadoEleccion+")");
				}
			}
		}

	}
	
	// Recibimos un mensaje entrante de Ok por lo que liberamos al proceso de la espera
	public void OK (boolean oK) {
		
		if(this.estado== true) {
				OK = oK;
				System.out.println("Proceso " + number + ": Recibo ok");			
				semOK.release();
		}
	}
	
	// Recibimos un mensaje entrante de nuevo coordinador por lo que liberamos al proceso de la espera
	public void Coordinador (int  Coord) {
		
		if(this.estado== true) {			
			this.coordinador = Coord;
			this.recibidoCoordinador = true;
			System.out.println("Proceso " + number + ": Recibido Nuevo Coordinador "+ Coord);
			semCoord.release();
		}
	}


	/********************************** GETTERS AND SETTERS **********************************/
	
	public int getNumber() {
		return number;
	}
	public void setNumProcT(int num) {
		this.numProcT = num;
	}
	public int getCoordinador() {
		return coordinador;
	}
	public int getEstadoEleccion() {
		return estadoEleccion;
	}

	/********************************** CONTROL DE HILOS **********************************/
	
	
	// Permite detener al proceso cambiando el atributo estado
	public void Detener() {
		this.estado=false;
	}
	
	// Permite la reanudación del proceso liberándolo del semáforo si se encuentra en estado parado
	public void Reanudar() {		
		if(this.estado==false){
			apagado.release(1);
		}	
	}

	// Finaliza la ejecución del proceso en cualquier estado en el que se encuentre
	public void Salir() {		
		if(this.estado == true) {
			
			this.estado=false;
			this.acabo=true;
		
		}else if(this.estado == false) {
			apagado.release(1);
			this.acabo=true;
		}
	}
}
