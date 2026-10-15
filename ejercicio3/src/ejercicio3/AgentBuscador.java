/**
 * 
 */
package ejercicio3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * @author jantonio
 *
 */
public class AgentBuscador extends Agent {

	/**
	 * 
	 */
	private UrlDirectory urlSources;
	protected CyclicBehaviourBuscar webInfoSearcher = null;
	
	private static final long serialVersionUID = 1L;
    
	public AgentBuscador(){
		super();
		urlSources= new UrlDirectory();
	}

	public void setup(){
		System.out.println("Soy el agente " + getLocalName());
		
		// Agrego los urls donde el comportamiento va a buscar la noticia .	
		urlSources.addUrl("https://www.elmundo.es/index.html");
		urlSources.addUrl("https://www.salamanca24horas.com");
		urlSources.addUrl("https://www.elpais.com");
    	
		// Publicando el servicio "Buscador".
		// Crear y registrar el servicio de tipo "buscar".	
		serviceRegister("Buscador", "buscar");
		System.out.println(" -Servicio con nombre Buscador, registrado...");
		// Crear y lanzar el comportamiento buscador en las urls.
		webInfoSearcher = new CyclicBehaviourBuscar(this, urlSources);
		addBehaviour(webInfoSearcher);
    }
	
	protected void takeDown(){
		// Desregistrar el servicio registrado de la p�ginas amarillas.
		try
		{
		 DFService.deregister(this);
		}
		catch(FIPAException e)
		{
		  e.printStackTrace();
		}
		
		System.out.println(" --- Agente " + getAID().getName() + " finalizando ---");
	}
	
	public void serviceRegister(String sName, String sType){
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
		sd.setName(sName);
		sd.setType(sType);
		sd.addOntologies("ontologia");
		sd.addLanguages(new SLCodec().getName());
		
		dfd.addServices(sd);
		
		// Registro del servicio...
		try
		{
			DFService.register(this, dfd);
		}
		catch(FIPAException e)
		{
			System.err.println(" -Agente " + getLocalName() + 
					": " + e.getMessage());
		}
	}

}// Fin de la clase AgentBuscador.


class CyclicBehaviourBuscar extends CyclicBehaviour{

	/**
	 * 
	 */
	private UrlDirectory myUrlSources;
	MessageTemplate mt;
	
	private static final long serialVersionUID = 1L;

	public CyclicBehaviourBuscar(Agent ownerAgent, UrlDirectory urls){
	 	super(ownerAgent);
	 	myUrlSources = urls;
	 	mt = MessageTemplate.and(
	    		MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
	    		MessageTemplate.MatchOntology("ontologia"));
	}
	
	@Override
	public void action() {
		// TODO Auto-generated method stub
		// Recibir solicitudes de b�squeda de los agentes agentCliente.
		ACLMessage inputMsg = myAgent.receive(mt);
		
		if (inputMsg != null){
			List<String> newsList = null;
			System.out.println(" -Realizando b�squeda de informaci�n...");
			try {
				newsList = buscarCadena((String)inputMsg.getContentObject());
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Enviar notificaci�n al agentCliente con los resultados de la 
			// b�squeda.
			System.out.println(" -B�squeda concluida, mandando confirmaci�n...");
			ACLMessage outputMsg = new ACLMessage(ACLMessage.INFORM);
			outputMsg.setSender(myAgent.getAID());
			outputMsg.addReceiver(inputMsg.getSender());
			outputMsg.setOntology("ontologia");
			//el lenguaje que se define para el servicio
			outputMsg.setLanguage(new SLCodec().getName());
			//el mensaje se transmita en XML
			outputMsg.setEnvelope(new Envelope());
	        //cambio la codificacion de la carta
			outputMsg.getEnvelope().setPayloadEncoding("ISO8859_1");
			
			try 
			{
				outputMsg.setContentObject((Serializable) newsList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			myAgent.send(outputMsg);
		} 
		else {
		  System.out.println(" -Agente esperando una solicitud de b�squeda...");	
	      block();
		}
	}
	
	public List<String> buscarCadena(String cadena)
	{
		//String sitios[]={"http://www.elmundo.es/index.html","http://www.salamanca24horas.com/sociedad","http://www.elpais.com"};
		URL url;
		List<String> lista=new ArrayList<String>();
		Scanner scanner;
		String temp;
		
		for(int i=0;i<myUrlSources.getDirectorySize();i++)
		{
			try 
			{
				url=new URL(myUrlSources.getUrl(i));
				scanner=new Scanner(url.openStream());
				
		        while(true)
		        {
					temp=scanner.findWithinHorizon(Pattern.compile("[^><\"]*"+
		                                                 cadena+"[^><\"]*"), 0);
		            
		            if(temp==null)
		                break;
		            
		            lista.add(temp);
		        }
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		return lista;
	}
} // Fin de la clase CyclicBehaviourBuscar.