package Control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class Gestor {

	
	public static void main(String[] args) throws NumberFormatException, IOException 
	{
		
		int opcion=0, opcion2=0;			// Variables para recoger las opciones escogidas por el usuario
		int valores=0;						// Variables para los valores en las entradas de teclado
		int i=0,numServ=-1;					// Variable para bucles y numero de servicios
		int u=0;   							// Variable para iterar los procesos en los bucles
		boolean flag=true,flag2=true; 		// Banderas usadas para el control de bucles while
		String ruta =new String("");		// Cadena que almacenará la ruta del fichero ini.txt
		int contadorProc=0;					// Contador de procesos
		
		BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in));		// Variable para leer desde teclado
		
		// Arrays
		ArrayList<String> listaIPs = new ArrayList();										// Lista dinámica de cadenas que contiene la lista de IPs de los servicios
        ArrayList<ArrayList<Integer>> listaprocs = new ArrayList<ArrayList<Integer>>();		// Lista de Listas enteras que contiene los procesos de cada servicio según su indice de lista
        ArrayList<Integer> Vivos = new ArrayList();											// Lista dinámica de enteros que contiene el estado en el que se encuentran los procesos
        ArrayList<Integer> Coordinadors = new ArrayList();									// Lista dinámica de enteros que contiene el coordinador que tiene cada proceso
        ArrayList<Integer> estadoelec = new ArrayList();									// Lista dinámica de enteros que contiene el estado de la elección que tiene cada proceso 

		// Inicializamos el cliente para poder hacer las llamadas REST
		ClientConfig config = new DefaultClientConfig();
	    Client client = Client.create(config);
	    URI uri = UriBuilder.fromUri("http://:8080/AlgoritmoBully").build();
	    WebResource service = client.resource(uri);
		

	    /************************** COMIENZO DEL PROGRAMA *****************************/
	    
	    System.out.print( "***************************************\n" +
	    				  "****                               ****\n" +
	    				  "****        ALGORITMO BULLY        ****\n" +
	    				  "****     (Sistemas distribuidos)   ****\n" +
	    				  "****                               ****\n" +
	    				  "***************************************\n" + "\n");
	    
	    System.out.print("Para el correcto funcionamiento del programa necesita un archivo de texto llamado \"ini.txt\". Este" +
	    				 "archivo debe tenerla siguiente estructura:\n" +
	    				 "\tIP Servicio\n" +
	    				 "\tID(proceso),ID(proceso),ID(proceso)...\n" +
	    				 "\tIP Servicio\n" +
	    				 "\tID(proceso),ID(proceso),ID(proceso)...\n" +
	    				 "\tIP Servicio\n" +
	    				 "\tID(proceso),ID(proceso),ID(proceso)...\n" +
	    				 "\nPodemos incluir tantos servicios como se necesiten y tantos procesos como se deseen en cada servicio.\n");
	    
	    
		/************************** LECTURA DESDE UN FICHERO DE TEXTO *****************************/
	    
	    //Introducimos la ruta del archivo ini.txt
	    while (flag==true)	{		
	    	
			System.out.print("\nIntroduzca la ruta del fichero ini.txt:\n"+ 
							"\t1) Ruta por defecto\n" +
							"\t2) Introducir ruta\n");
			opcion2 = Integer.parseInt( entrada.readLine() );
			switch(opcion2){
					
				case 1:
					ruta = "/home/i2968553/Desktop/ini.txt";
					flag=false;
					break;
				case 2:
					System.out.print("Introduzca la ruta:");
					ruta = entrada.readLine();
					flag=false;
					break;
				default:
					System.out.print("Opcion incorrecta. Intentelo de nuevo");
					break;
		 	}
		}
	    
	    //Leemos el archivo ini.txt y se van creando los servicio y los procesos asociados a cada servicio
		try{
					 	
			File fich = new File (ruta);
	    	Scanner scn = new Scanner (fich);
	    	int contbucle=1;
	    	//numServ=-1;QUITAR
			while (scn.hasNextLine()){
				String linea = scn.nextLine();
				Scanner sl = new Scanner(linea);
				sl.useDelimiter("\\s*,\\s*");		
				switch(contbucle) {
				
					case 1: 
						//System.out.println(linea);
						listaIPs.add(linea);
						listaprocs.add(new ArrayList<Integer>());
						contbucle++;
						numServ++;
					    break;
											    	 
					case 2: 
						contbucle=1;
						uri=UriBuilder.fromUri("http://"+ listaIPs.get(numServ) +":8080/AlgoritmoBully").build();
					    service = client.resource(uri);
								    
						while(sl.hasNext() ==true){
							
								valores=Integer.parseInt(sl.next());
								//System.out.println(valores);
								System.out.print(service.path("rest/servicio/newproceso").queryParam("id",""+valores).queryParam("estado",""+1).queryParam("coord",""+(-1)).accept(MediaType.TEXT_PLAIN).get(String.class) + "\n");
								listaprocs.get(numServ).add(valores);
								contadorProc++;
								Vivos.add(1);
								Coordinadors.add(-1);
								estadoelec.add(0);
						}
						break;			
								
				}//switch
			}//while
			scn.close();
					 
		}catch (FileNotFoundException fnfe){
		   	fnfe.printStackTrace();
		}
		
		
		//Mostramos la tabla de correspondencias IPs y procesos
		System.out.print("\nTabla de IPs de servicios y procesos:\n");
		u=0;
		for(String k: listaIPs) {
			System.out.print("\t"+ u +") " + k + "\n");
			for(Integer h: listaprocs.get(u)) {
				System.out.print("\t" + h + "   ");
			}
			System.out.print("\n");
			u++;
		
		}
		
		
		// Le pasamos a cada servicio el numero total de procesos para que se lo notifique a cada proceso asociado a �l
		// También enviamos a cada servicio las tablas de IPs y procesos
		for(i=0;i<=numServ;i++) {
			 
			System.out.print("\n Para el servicio " + listaIPs.get(i) + "\n");
			uri=UriBuilder.fromUri("http://"+ listaIPs.get(i) +":8080/AlgoritmoBully").build();
			service = client.resource(uri);
						 
			System.out.print("\t" + service.path("rest/servicio/numtotalprocesos").queryParam("numtotal",""+contadorProc).accept(MediaType.TEXT_PLAIN).get(String.class)+"\n");
						 			    	
			u=0;
			for(String k: listaIPs) {
				    	
			   	System.out.print("\t" + service.path("rest/servicio/asignarip").queryParam("ip",""+listaIPs.get(u)).accept(MediaType.TEXT_PLAIN).get(String.class)+"\n");
						
			    for(Integer h: listaprocs.get(u)){
			
			    	System.out.print("\t" + service.path("rest/servicio/asignarid").queryParam("indice",""+u).queryParam("id",""+h).accept(MediaType.TEXT_PLAIN).get(String.class)+"\n");
			    }//for
			    u++;
			}//for
		}//for
					 
		//Mandamos a todos los servicios que inicien los procesos asociados a �l			
		for(i=0;i<=numServ;i++)  {
						 	
			//System.out.print("\n NumSERV:"+ numServ + " LA I vale " + i );
		   	uri=UriBuilder.fromUri("http://"+ listaIPs.get(i) +":8080/AlgoritmoBully").build();
			service = client.resource(uri);
		   	System.out.print("\n Servicio "+ listaIPs.get(i) + " :" + service.path("rest/servicio/iniciar").accept(MediaType.TEXT_PLAIN).get(String.class) + "\n");
					
		}

		
	    
		/************************** CONTROL DE EJECUCIÓN DE PROCESOS *****************************/	
		
		/* En este apartado podemos parar un proceso concreto, reanudarlo y actualizar en la tabla los coordinadores
		 * y el estado de la elección de cada proceso
		 */
		while (flag2==true) {
		
			System.out.print( "\nEliga lo que desea hacer:\n"+ 
								"\t1) Detener Proceso\n" +
								"\t2) Reanudar Proceso\n" +
								"\t3) Actualizar coordinadores y estado de la elección\n" +
								"\t4) Salir\n");
								 
			System.out.print( "\nESTADO Y COORDINADORES DE LOS PROCESOS DE LOS PROCESOS\n");
			System.out.print( "*****************************************************************\n");
			System.out.print( "***************** Estado **** Coordinador **** Estado elección **\n");
			
			for(i=0; i<contadorProc; i++) {
				
				System.out.print( "** Proceso  "+ (i+1) + "   =  ");
				if(Vivos.get(i) == 1)
					System.out.print("  ON " + "  **");
				else if(Vivos.get(i) == 0)
					System.out.print("  OFF " + " **");
				
				if(Vivos.get(i) == 0)
					System.out.print("      OFF " + "     **");
				else
					System.out.print("      "+Coordinadors.get(i) + "        **");
				
				if(Vivos.get(i) == 0)
					System.out.print("       OFF " + "       **\n");
				else
					System.out.print("         "+estadoelec.get(i) + "        **\n");
				
			}
			System.out.print( "*****************************************************************\n");
			System.out.print( "*****************************************************************\n");
			
			
			opcion = Integer.parseInt( entrada.readLine() );
			
			switch(opcion){				
				
				case 1:
					
					
					System.out.print( "\nIntroduzca el ID del proceso:");
					opcion2 = Integer.parseInt( entrada.readLine() );
					
					u=0;
					flag=true;
					for(String k: listaIPs) {
						
						for(Integer h: listaprocs.get(u)) {
							
							if (h == opcion2) {
								uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
							    service = client.resource(uri);
							    System.out.print("\n" + service.path("rest/servicio/detener").queryParam("proceso",""+opcion2).accept(MediaType.TEXT_PLAIN).get(String.class));
							    System.out.println("Mato a ("+opcion2+") que es " + h+ "en ip " + k + "\n");
							    flag=false;
							    Vivos.set(opcion2-1,0);
							}
						}
						u++;
						if(flag==false)
							break;
						
					}
					break;
	
				case 2:
					
					System.out.print( "\nIntroduzca el ID del proceso:");
					opcion2 = Integer.parseInt( entrada.readLine() );
					
					u=0;
					flag=true;
					for(String k: listaIPs) {
						
						for(Integer h: listaprocs.get(u)) {
							
							if (h == opcion2) {
								uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
							    service = client.resource(uri);
							    System.out.print("\n" + service.path("rest/servicio/reanudar").queryParam("proceso",""+opcion2).accept(MediaType.TEXT_PLAIN).get(String.class));
							    System.out.println("REvivio a ("+opcion2+") que es " + h+ "en ip " + k + "\n");
							    flag=false;
							    Vivos.set(opcion2-1,1);
							    break;
							}
							
						}
						u++;
						if(flag==false)
							break;
					}
					break;					
				
				
				case 3:
					
					u=0;
					for(String k: listaIPs) {
						
						for(Integer h: listaprocs.get(u)) {
							
								uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
							    service = client.resource(uri);
							    Coordinadors.set(h-1,Integer.parseInt(service.path("rest/servicio/obtenercoord").queryParam("proceso",""+h).accept(MediaType.TEXT_PLAIN).get(String.class)));
						}
						u++;	
					}
					
					u=0;
					for(String k: listaIPs) {
						
						for(Integer h: listaprocs.get(u)) {
							
								uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
							    service = client.resource(uri);
							    estadoelec.set(h-1,Integer.parseInt(service.path("rest/servicio/estadoelección").queryParam("proceso",""+h).accept(MediaType.TEXT_PLAIN).get(String.class)));
						}
						u++;	
					}
					
					break;
					
				
				case 4:					
					u=0;
					for(String k: listaIPs) {
						
						for(Integer h: listaprocs.get(u)) {
							
								uri=UriBuilder.fromUri("http://"+ k +":8080/AlgoritmoBully").build();
							    service = client.resource(uri);
							    System.out.print(service.path("rest/servicio/Salir").accept(MediaType.TEXT_PLAIN).get(String.class) + "\n");
						}
						u++;	
					}
					flag2=false;
					break;
					
					
				default:
					System.out.print("\nError opcion incorrecta\n");	
			}
		
		}
			
	}
}