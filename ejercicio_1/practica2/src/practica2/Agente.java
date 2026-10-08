package practica2;

import jade.core.Agent;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.Agent;

import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.JOptionPane;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

public class Agente extends Agent {

	private static final long serialVerisonUID = 1L;
	
	protected ParallelBehaviour compoundBehaviour;
	protected OneShotBehaviourEnviar messageSender = null;
	protected CyclicBehaviourImprimir messageReceiver = null;
	
	private ThreadedBehaviourFactory thrBFSender = new ThreadedBehaviourFactory();
	private ThreadedBehaviourFactory thrBFReciever = new ThreadedBehaviourFactory();
	
	public void setup() {
		System.out.println("Soy el agente " + getLocalName());
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName("ServicioMensajeria");
		sd.setType("Mensajeria");
		sd.addOntologies("Ontologia");
		sd.addLanguages(new SLCodec().getName());
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		}
		catch(FIPAException e) {
			System.err.println(" -Agente" + getLocalName() + ": " + e.getMessage());
		}
		
		compoundBehaviour = new ParallelBehaviour(this, ParallelBehaviour);
		messageSender = new OneShotBehaviourEnviar(this);
		messageReceiver = new CyclicBehaviourImprimir(this);
		//addBehaviour //COMPORTAMIENTO COMPUESTO? 
		//addBehaviour
		compoundBehaviour.addSubBehaviour(thrBFSender.wrap(messageSender));
		compoundBehaviour.addSubBehaviour(thrBFReciever.wrap(messageReceiver));
		addBehaviour(compoundBehaviour);
	}
	
	// FALTAN COSAS AQUI
	
	public void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch(FIPAException e) {
			e.printStackTrace();
		}
		
		System.out.println(" --- Agnete " +
		getAID().getName() + " finalizando ---");
	}
	
}

class OneShotBehaviourEnviar extends OneShotBehaviour {
	
	private static final long serialVerionUID = 1L;
	
	public OneShotBehaviourEnviar(Agent agent) {
		super(agent);
	}
	
	// @Override
	public void action() {
		System.out.println("-- Comportamiento OneShotBehaviourEnviar --");
		
		AID[] arrAgentIds = null;
		String userMsg = null;
		
		System.out.println(" - Entre un mensaje " +
				"para enviarlo a otros agentes: ");
		BufferedReader buffer = new BufferedReader (new InputStreamReader (System.in));
		
		try {
			userMsg = buffer.readLine();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		arrAgentIds = searchServiceAgents("Mensajeria");
		if(arrAgentIds != null) {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(myAgent.getAID());
			for (int i = 0; i < arrAgentIds.length; i++)
				msg.addReceiver(arrAgentIds[i]);
			msg.setContent(userMsg);
			msg.setOntology("ontologia");
			msg.setLanguage(new SLCodec().getName());
			
			myAgent.send(msg);
		}
	}
	
	private AID[] searchServiceAgents(String msgType) {
		AID[] agentIds = null;
		
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType(msgType);
		template.addServices(templateSd);
		
		try {
			DFAgentDescription[] arrResult = DFService.search(myAgent, template);
			
			if(arrResult.length > 0) {
				System.out.println(" -Agente " + myAgent.getAID().getLocalName() + " ha encontrado los sgtes agentes: ");
				agentIds = new AID[arrResult.length];
				for (int i = 0; i < arrResult.length; i++) {
					agentIds[i] = arrResult[i].getName();
					System.out.println(" -Agente " + ": Tipo de servicio -> Mensajería.");
				}
			}
			else {
				JOptionPane.showMessageDialog(null,  "Agente " + myAgent.getAID().getLocalName() + " no encontró nungún servicio", "Error", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		catch(FIPAException e) {
			JOptionPane.showMessageDialog(null,  "Agente " + myAgent.getAID().getLocalName() + ": " + e.getMessage(), "Error", JOptionPane.INFORMATION_MESSAGE);
			e.printStackTrace();
		}
		return agentIds;
	}
}

class CyclicBehaviourImprimir extends CyclicBehaviour {
	
	private static final long serialVerisonUID = 1L;
	
	public CyclicBehaviourImprimir(Agent agnet) {
		super(agent);
	}
	
	@Override
	public void action() {
		System.out.println("-- Comportamiento CyclicBehaviourImprimir --");
		
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		ACLMessage msg = myAgent.clockingReceive(mt);
		if (msg != null) {
			String strContent = msg.getContent();
			System.out.println("Agnete " + myAgent.getAID().getLocalName() + " recibió un mensaje del agente " + msg.getSender().getLocalName() + ": ");
			System.out.println(" -Mensaje: " + strContent);
		}
	}
}