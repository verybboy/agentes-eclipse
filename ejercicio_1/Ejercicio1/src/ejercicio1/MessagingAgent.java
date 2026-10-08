package ejercicio1;

import jade.core.Agent;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

//import java.util.*;

import javax.swing.JOptionPane;

import java.io.*;

public class MessagingAgent extends Agent {

	/**
	 * José A. Castellanos Garzón
	 * Versión 1.0, curso 2018/2019
	 */
	private static final long serialVersionUID = 1L;
	
	protected ParallelBehaviour compoundBehaviour ; 
	protected OneShotBehaviourEnviar messageSender = null;
	protected CyclicBehaviourImprimir messageReceiver = null;
	
	private ThreadedBehaviourFactory thrBFSender = new ThreadedBehaviourFactory();
	private ThreadedBehaviourFactory thrBFReceiver = 
            								   new ThreadedBehaviourFactory();
		
	public void setup(){
		System.out.println("Soy el agente " + getLocalName());
		
		// Crear y registrar el servicio de "mensajería"	
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName("ServicioMensajeria");
		sd.setType("Mensajeria");
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
		
		// Crear y adicionar los comportamientos
		compoundBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
		messageSender = new OneShotBehaviourEnviar(this);
		messageReceiver = new CyclicBehaviourImprimir(this);
		//addBehaviour(thrBFSender.wrap(messageSender));
		//addBehaviour(thrBFReceiver.wrap(messageReceiver));
		compoundBehaviour.addSubBehaviour(thrBFReceiver.wrap(messageReceiver));
		compoundBehaviour.addSubBehaviour(thrBFSender.wrap(messageSender));
		
	    addBehaviour(compoundBehaviour);
	}
	
	protected void takeDown(){
		// Desregistrar el servicio registrado de la páginas amarillas.
		try
		{
		 DFService.deregister(this);
		 // Destruir los hilos de los comportamientos en paralelo.
		 //if (thrBFSender != null)
		 //   thrBFSender.getThread(messageSender).interrupt();
		 //if (thrBFReceiver != null) 
		 //   thrBFReceiver.getThread(messageReceiver).interrupt();
		}
		catch(FIPAException e)
		{
		  e.printStackTrace();
		}
		
		System.out.println(" --- Agente " + getAID().getName() + " finalizando ---");
	}

} // Fin de la clase MessagingAgent.

class OneShotBehaviourEnviar extends OneShotBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    
    public OneShotBehaviourEnviar(Agent agent){
     super(agent);	
    }
    
	@Override
	public void action() {
		// TODO Auto-generated method stub
		System.out.println("-- Comportamiento OneShotBehaviourEnviar --> " +
		                   myAgent.getAID().getLocalName());
		
	    AID[] arrAgentIds = null;
	    String userMsg = null;

		System.out.println(" -Escriba un mensaje para enviarlo a otros agentes: ");
		BufferedReader buffer = new BufferedReader(new InputStreamReader(
				                                   System.in));
	    try 
	    {
		  userMsg = buffer.readLine();
		} 
	    catch (IOException e) 
	    {
			// TODO Auto-generated catch block
		  e.printStackTrace();
		}
	    
	    // Encontrar los agentes que implementan el servicio y 
	    // enviarles el mensaje.
		arrAgentIds = searchServiceAgents("Mensajeria");
		if (arrAgentIds != null){
		  ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		  msg.setSender(myAgent.getAID());
		  for (int i = 0; i < arrAgentIds.length; i++)
			msg.addReceiver(arrAgentIds[i]);
		  msg.setContent(userMsg);
		  msg.setOntology("ontologia");
		  msg.setLanguage(new SLCodec().getName());
		  
		  if (msg.getSender() == null)
			  System.out.println(" -El campo sender es nulo.");  
		  else
		      System.out.println(" -Agente " + msg.getSender().getName() + 
		    		  " enviando mensaje.");
		  
		  myAgent.send(msg);
		}
	}
	
	private AID[] searchServiceAgents(String msgType){
		AID[] agentIds = null;
		
		// Crear las características del servicio a buscar.
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType(msgType); // La búsqueda por el tipo de servicio.
		template.addServices(templateSd);
		
		//SearchConstraints sc = new SearchConstraints();
		//sc.setMaxDepth(new Long(1));
		//sc.setMaxDepth(Long.MAX_VALUE);
		
		// Buscando los agentes que implementan el tipo de servicio.
		try
		{
		   DFAgentDescription[] arrResult = DFService.search(myAgent, template); 	
		   if (arrResult.length > 0){
			 System.out.println(" -Agente " + myAgent.getAID().getLocalName() + 
					            " ha encontrado los sgtes agentes: ");
			 agentIds = new AID[arrResult.length];
			 for (int i = 0; i < arrResult.length; i++){
				agentIds[i] = arrResult[i].getName();
				System.out.println("  -Agente " + agentIds[i].getName() + 
						           ": Tipo de servicio -> Mensajeria.");
			 }
		   } 
		   else{
			JOptionPane.showMessageDialog(null, "Agente " + 
		                                  myAgent.getAID().getLocalName() +
		                                  " no encontró ningún servicio", 
		                                  "Error", 
		                                  JOptionPane.INFORMATION_MESSAGE);   
		   }
			   
		}
		catch(FIPAException e)
		{
		  JOptionPane.showMessageDialog(null, "Agente " + 
                                        myAgent.getAID().getLocalName() +
                                        ": " + e.getMessage(), "Error", 
                                        JOptionPane.INFORMATION_MESSAGE);	
		  e.printStackTrace();
		}
		
		return agentIds;	
	}
	
} // Fin de la clase OneShotBehaviourEnviar.

class CyclicBehaviourImprimir extends CyclicBehaviour {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    
    public CyclicBehaviourImprimir(Agent agent){
     super(agent)	;
    }
    
	@Override
	public void action() {
		System.out.println("-- Comportamiento CyclicBehaviourImprimir --> " +
				           myAgent.getAID().getLocalName());
		// TODO Auto-generated method stub
	    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		//ACLMessage msg = myAgent.receive(mt);
		ACLMessage msg = myAgent.blockingReceive(mt);
	    if (msg != null){
		 String strContent = msg.getContent();
		 System.out.println("Agente " + myAgent.getAID().getLocalName() + 
				            " recibió un mensaje del agente " + 
				            msg.getSender().getLocalName() + ": ");
		 System.out.println(" -Mensaje: " + strContent);
	    }
	  //else block();
	}
	
}// Fin de la clase CyclicBehaviourImprimir.