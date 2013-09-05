package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

import domain.Execution;
import domain.Id;
import domain.Phase;
import domain.TimedNeighbor;
import domain.network.FakeNode;

public class ExecutionTest {

	FakeNode localNode;
	Execution execution;
	static final int NUM_OF_ROUNDS = 100;

	double[] impulseResponses = { 0.0122, 0.0068, 0.0686, 0.1061, 0.0763,
			-0.0071, -0.0985, -0.1537, -0.1534, -0.1074, -0.0426, 0.0131,
			0.0439, 0.0488, 0.0373, 0.0217, 0.0103, 0.0055, 0.0050, 0.0051,
			0.0033, -0.0007 };

	double[][] expectedMatrixA = {
			{ 0.8216387101399824, -0.4968986018097526, -0.10307764582740445,
					-0.0038718697884600577, -0.0023294699538915354,
					-1.9724459372758816E-4, -3.267636269546181E-5,
					2.5648778471495254E-4, -1.8363828674442776E-4,
					-6.571688683938801E-5, 2.6412689195783197E-5 },
			{ 0.4968976683986355, 0.7770613240226641, -0.1688004704144454,
					0.2059518247363174, -0.0022257809932847636,
					-4.442525941294531E-4, 2.0987878434335343E-5,
					3.565983235325936E-4, 6.282000261760494E-4,
					-5.650252933786578E-4, 1.4041251948703246E-4 },
			{ 0.10307852509438245, -0.168808307686376, 0.7788395703293963,
					0.450104755855965, -1.0131859468495057E-4,
					1.2158612503467762E-4, -3.095123541637713E-5,
					-2.817315426603456E-5, -3.6996771174662476E-4,
					2.2808365962516733E-4, -5.512215715575613E-5 },
			{ 0.0038742603774503115, -0.2059740047707813, -0.45006379012592057,
					0.37994294545902996, 0.03850764340294818,
					0.0018460814379202112, 8.416404727687246E-4,
					-0.003668346017118882, 0.007365132634122893,
					-0.0016312300770859258, 1.9402414801539844E-4 },
			{ -0.0024445973172223523, 0.002774171245353063,
					5.513914573166967E-4, 0.03436744782050827,
					-0.02596179116387584, -0.44655489501077106,
					0.18118382034337588, -0.2648694036662246,
					0.09927634074843482, -0.1630230450676072,
					0.10750629565633527 },
			{ 2.842848893568478E-4, -4.030153291181282E-4,
					-6.480822642163653E-4, 1.1658578429685207E-4,
					0.39033145147395193, -0.617804168091744,
					-0.09169021409141737, 0.4545627169842995,
					-0.3829600974050032, 0.07142247050449155,
					-0.031423376198607515 },
			{ 1.7774310054167325E-4, 3.7519842896593936E-4,
					-0.0021090148020957294, 0.004827014391035944,
					0.008694051958294771, 0.06404210741899574,
					-0.3735412878779704, -0.6219847387983777,
					-0.5169034664645974, 0.13468674334689346,
					-0.0011054467354070733 },
			{ -1.8569015751301876E-4, 0.0013766026538490905,
					-0.00158345394563697, 0.002738297838304149,
					0.09746713594777112, 0.3625754068534086,
					0.8218749155059852, 0.05355118750608526,
					-0.41749808842634034, -0.0010567806177222971,
					0.08881988486950886 },
			{ -3.787795430239549E-4, -5.648038990111459E-4,
					0.0019487286047066288, 0.0025657534655949554,
					0.20610486666055666, 0.3684272628347598,
					-0.4197843395377061, 0.19944495194040218,
					-0.29649115128673, -0.49323806627464695,
					0.19844717850807453 },
			{ 4.042093889422005E-4, -6.173137407179397E-4,
					-0.0025647717418157967, 0.009808522383803808,
					-0.029396087459775427, 0.09125264671654684,
					-0.2901472888784973, 0.37280390033328153,
					0.4811796279233361, -0.2289295315974148,
					0.35141657948883304 },
			{ 7.792065309675245E-5, 7.039948038967125E-4,
					-0.0011727841462543243, -7.44664517289273E-4,
					-0.026904846087561496, -0.04504176779558322,
					0.16738728149611443, -0.09658033314506323,
					0.01841492030464989, -0.6605181251852452,
					-0.7692134536834554 } };

	double[] expectedEigenvals = { 0.9328880467738535, 0.871627869420588,
			0.8463250407483807, 0.825305912999191, 0.46190743101925374,
			1.2048044030847112E-16 };

	@Before
	public void setUp() throws Exception {
		localNode = new FakeNode();
	}

	@Test
	public void initValueIsOnlyOneOrZero() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		ArrayList<Double> expected = new ArrayList<Double>();
		expected.add(0.0);
		expected.add(1.0);
		assertTrue(expected.contains(execution.getImpulseResponse(1)));
	}

	@Test
	public void outTableHasProperWeights() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		Id nodeId = new Id(FakeNode.getNode1());
		double weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0 / 3, weight, 0);
	}

	@Test
	public void checkIfWeightsAreRecomputed() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		Id nodeId = new Id(FakeNode.getNode1());
		double weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0 / 3, weight, 0);
		localNode.renewOutNeighbors();
		execution.recomputeWeight();
		weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0 / 4, weight, 0);
	}

	@Test
	public void executionHasTerminated() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		for (int i = 2; i <= NUM_OF_ROUNDS + 1; i++) {
			execution.setRound(i);
		}
		assertTrue(execution.hasTerminated());
	}

	@Test
	public void canAccessCorrectRound() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		execution.setCurrentImpulseResponse(10.0);
		assertEquals(10.0, execution.getImpulseResponse(1), 0);

		execution.setImpulseResponse(5, 5.0);
		assertEquals(5.0, execution.getImpulseResponse(5), 0);

		execution.setCurrentValue(20.0);
		assertEquals(20.0, execution.getCurrentValue(), 0);

		execution.addValToRound(13.0, 10);
		assertEquals(13.0, execution.getValsOfRound(10), 0);
	}

	@Test
	public void addANewInNeighbor() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte[] id = { 1, 2, 3, 4, 6, 7, 8, 9, 0 };
		Id i = new Id(id);
		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn = new TimedNeighbor(i, address);
		assertTrue(execution.addInNeighbor(tn));
		assertEquals(tn, execution.getInNeighbors().getNeighbor(i));
	}

	@Test
	public void addAnAlreadyExistingNeighbor() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte[] id = { 1, 2, 3, 4, 6, 7, 8, 9, 0 };
		Id i = new Id(id);
		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn = new TimedNeighbor(i, address);
		execution.addInNeighbor(tn);
		assertFalse(execution.addInNeighbor(tn));
		assertEquals(1, execution.getInNeighbors().getSize());
	}

	@Test
	public void setNewRoundProperly() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);

		execution.addValToRound(10, 2);
		execution.addValToRound(20, 3);
		execution.addValToRound(30, 2);

		execution.setRound(2);
		assertEquals(40.0, execution.getCurrentValue(), 0);
		execution.setRound(4);
		assertEquals(0.0, execution.getCurrentValue(), 0);
		execution.setRound(3);
		assertEquals(20.0, execution.getCurrentValue(), 0);
	}

	@Test
	public void checkRemainingTime() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(execution.remainingInitTime() <= 10 * ProtocolController.TIMEOUT - 10);
	}

	@Test
	public void computeRealizationMatrixProperly() {
		execution = new Execution(1, impulseResponses.length, localNode);

		// The matrix should not be computed yet
		assertNull(execution.computeRealizationMatrix());

		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i - 1], i);
		}

		for (int i = 2; i <= impulseResponses.length + 1; i++) {
			execution.setRound(i);
		}
		// Must do this, otherwise the first value will always be 0 or 1
		// We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		assertFalse(execution.hasAnotherRound());

		DoubleMatrix m = execution.computeRealizationMatrix();

		for (int i = 0; i < m.getRows(); i++) {
			for (int j = 0; j < m.getColumns(); j++) {
				assertEquals(expectedMatrixA[i][j], m.get(i, j), 0.05);
			}
		}
	}

	@Test
	public void returnRealizationMatrixOnlyOnceComputed() {
		execution = new Execution(1, impulseResponses.length, localNode);

		// The matrix should not be computed yet
		assertNull(execution.getRealizationMatrix());

		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i - 1], i);
		}

		for (int i = 2; i <= impulseResponses.length + 1; i++) {
			execution.setRound(i);
		}
		// Must do this, otherwise the first value will always be 0 or 1
		// We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		assertFalse(execution.hasAnotherRound());

		execution.computeRealizationMatrix();

		DoubleMatrix m = execution.getRealizationMatrix();

		for (int i = 0; i < m.getRows(); i++) {
			for (int j = 0; j < m.getColumns(); j++) {
				assertEquals(expectedMatrixA[i][j], m.get(i, j), 0.05);
			}
		}
	}

	@Test
	public void returnEigenvaluesOnceMatrixIsComputed() {
		execution = new Execution(1, impulseResponses.length, localNode);

		// The matrix should not be computed yet
		assertNull(execution.getMatrixAEigenvalues());

		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i - 1], i);
		}

		for (int i = 2; i <= impulseResponses.length + 1; i++) {
			execution.setRound(i);
		}
		// Must do this, otherwise the first value will always be 0 or 1
		// We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);

		execution.computeRealizationMatrix();

		double[] eigenvalues = execution.getMatrixAEigenvalues();
		
		assertArrayEquals(expectedEigenvals, eigenvalues, 0.05);

	}

	@Test
	public void hasAdditionalRounds() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		assertTrue(execution.hasAnotherRound());
		for (int i = 2; i <= NUM_OF_ROUNDS; i++) {
			execution.setRound(i);
		}
		assertFalse(execution.hasAnotherRound());
	}

	@Test
	public void medianEigenvaluesAreComputedCorrectly() {

		double[] newEigenvalues = { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
		byte[] i1 = { 1, 2, 3, 3, 2, 1 };
		byte[] i2 = { 3, 2, 1, 1, 2, 3 };

		Id id1 = new Id(i1);
		Id id2 = new Id(i2);

		execution = new Execution(1, impulseResponses.length, localNode);

		// The matrix should not be computed yet
		assertNull(execution.getMatrixAEigenvalues());
		assertFalse(execution.setMatrixAEigenvalues(newEigenvalues));
		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i - 1], i);
		}

		for (int i = 2; i <= impulseResponses.length + 1; i++) {
			execution.setRound(i);
		}
		// Must do this, otherwise the first value will always be 0 or 1
		// We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);

		// Computation should be null because we are not in the gossip phase
		assertNull(execution.computeMedianEigenvalues());

		execution.computeRealizationMatrix();
		execution.setPhase(Phase.GOSSIP);
		execution.addGossipEigenvalues(id1.toString(), newEigenvalues);
		execution.addGossipEigenvalues(id2.toString(), newEigenvalues);

		// The eigenvalues should be the new ones, since we have 2 times these
		// and only one time the expected
		double[] median = execution.computeMedianEigenvalues();
		assertArrayEquals(newEigenvalues, median, 0.05);

		// Now the second remote nodes has the expected eigenvalues,
		// thus the median should be the expected one
		execution.addGossipEigenvalues(id2.toString(), expectedEigenvals);
		median = execution.computeMedianEigenvalues();
		assertArrayEquals(expectedEigenvals, median, 0.05);
	}

	@Test
	public void checkIfRoundIsOver() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte[] id1 = { 1, 2, 3, 4, 6, 7, 8, 9, 0 };
		byte[] id2 = { 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
		byte[] id3 = { 1, 1, 2, 2, 3, 3, 4, 4, 5, 5 };

		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn1 = new TimedNeighbor(id1, address);
		TimedNeighbor tn2 = new TimedNeighbor(id2, address);
		TimedNeighbor tn3 = new TimedNeighbor(id3, address);

		execution.addInNeighbor(tn1);
		execution.addInNeighbor(tn2);
		execution.addInNeighbor(tn3);

		assertFalse(execution.roundIsOver());
		execution.setTimerToInf(tn1.getId().toString());
		assertFalse(execution.roundIsOver());
		execution.setTimerToInf(tn2.getId().toString());
		execution.setTimerToInf(tn3.getId().toString());
		assertTrue(execution.roundIsOver());
	}

}
