package practica2;

import jade.core.Agent;

import jade.core.Agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
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
		
		compoundBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
		messageSender = new OneShotBehaviourEnviar(this);
		messageReceiver = new CyclicBehaviourImprimir(this);
		compoundBehaviour.addSubBehaviour(thrBFSender.wrap(messageSender));
		compoundBehaviour.addSubBehaviour(thrBFReciever.wrap(messageReceiver));
		addBehaviour(compoundBehaviour);
	}
	
	AID[] searchServiceAgents(String serviceType) {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		template.addServices(sd);

		try {
			DFAgentDescription[] results = DFService.search(this, template);
			AID[] agentIds = new AID[results.length];
			for (int i = 0; i < results.length; i++)
				agentIds[i] = results[i].getName();
			return agentIds;
		}
		catch (FIPAException e) {
			e.printStackTrace();
			return null;
		}
	}

	class CyclicBehaviourImprimir extends CyclicBehaviour {

		public CyclicBehaviourImprimir(Agent agent) {
			super(agent);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			if (msg != null) {
				System.out.println(" - Mensaje de " + msg.getSender().getLocalName() + ": " + msg.getContent());
			}
			else {
				block();
			}
		}
	}
	
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
		
		arrAgentIds = ((Agente) myAgent).searchServiceAgents("Mensajeria");
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
}
