package net;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.lang.Math;

public class net {

	public static Connection c = null;
	public static List<Integer> wordids = new ArrayList<Integer>();
	public static List<Integer> resids = new ArrayList<Integer>();
	public static List<Integer> hiddenids = new ArrayList<Integer>();
	public static Double[][] wi;
	public static Double[][] wo;
	public static Double[] ai;
	public static Double[] ah;
	public static Double[] ao;

	public void connectDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Opened database successfully");

	}

	public void closeDB() {
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void makeTable() {
		Statement stmt = null;
		try {
			stmt = c.createStatement();
			String sqlhiddennode = "CREATE TABLE hiddennode " + "(create_key CHAR(50))";
			stmt.executeUpdate(sqlhiddennode);

			String sqlwordhidden = "CREATE TABLE wordhidden " + " (fromid            INT     NOT NULL, "
					+ " toid        INT     NOT NULL, " + " strength         REAL)";
			stmt.executeUpdate(sqlwordhidden);

			String sqlhiddenres = "CREATE TABLE hiddenres " + "( fromid            INT     NOT NULL, "
					+ " toid        INT     NOT NULL, " + " strength         REAL)";
			stmt.executeUpdate(sqlhiddenres);

			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Table created successfully");

	}

	public double getstrength(int fromid, int toid, int layer) {
		float res = 0;
		String table;
		if (layer == 0)
			table = "wordhidden";
		else
			table = "hiddenres";
		String sql = "select strength from " + table + " where fromid= " + Integer.toString(fromid) + " and toid= "
				+ Integer.toString(toid) + ";";

		Statement stmt = null;
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				res = rs.getFloat("strength");
				// System.out.println("strength = " + res);
			} else {
				if (layer == 0)
					return -0.2;
				if (layer == 1)
					return 0;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return res;
	}

	public void setstrength(int fromid, int toid, int layer, double strength) {
		String table;
		if (layer == 0)
			table = "wordhidden";
		else
			table = "hiddenres";
		String sql = "select rowid from " + table + " where fromid= " + Integer.toString(fromid) + " and toid= "
				+ Integer.toString(toid) + ";";
		Statement stmt = null;
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				int id = rs.getInt("rowid");
				sql = "update " + table + " set strength = " + String.valueOf(strength) + " where rowid= "
						+ String.valueOf(id) + ";";
				stmt.executeUpdate(sql);
			} else {
				sql = "insert into " + table + " (fromid,toid,strength) " + "values ( " + Integer.toString(fromid) + ","
						+ Integer.toString(toid) + "," + String.valueOf(strength) + ") ; ";
				stmt.executeUpdate(sql);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public void generatehiddennode(List<Integer> wordid, List<Integer> res) {
		if (wordid.size() > 3)
			return;
		String creat_key = "";
		Collections.sort(wordid);
		for (int i = 0; i < wordid.size(); i++) {
			creat_key += wordid.get(i);
			creat_key += "_";
		}

		String sql = "select rowid from hiddennode where create_key=" + "'" + creat_key + "'" + ";";
		Statement stmt = null;
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (!rs.next()) {
				sql = "insert into hiddennode (create_key) values( " + "'" + creat_key + "'" + " ) ;";
				int hiddenid = rs.getRow() + 1;
				stmt.executeUpdate(sql);

				for (int i = 0; i < wordid.size(); i++)
					setstrength((int) wordid.get(i), hiddenid, 0, 1.0 / wordid.size());

				for (int j = 0; j < res.size(); j++)
					setstrength(hiddenid, (int) res.get(j), 1, 0.1);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public List<Integer> getallhiddenids(List<Integer> wordid, List<Integer> res) throws SQLException {
		Map<Integer, Integer> l1 = new HashMap<Integer, Integer>();
		String sql;
		Statement stmt = null;
		stmt = c.createStatement();
		for (int i = 0; i < wordid.size(); i++) {
			sql = "select toid , rowid from wordhidden where fromid= " + String.valueOf((int) wordid.get(i)) + ";";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt("toid");
				l1.put(id, 1);
			}
		}

		for (int j = 0; j < res.size(); j++) {
			sql = "select fromid ,rowid from hiddenres where toid= " + String.valueOf((int) res.get(j)) + ";";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				int id = rs.getInt("fromid");
				l1.put(id, 1);
			}
		}
		List<Integer> hinddenid = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> entry : l1.entrySet()) {
			hinddenid.add(entry.getKey());
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		return hinddenid;

	}

	public void setupnetwork(List<Integer> wordid, List<Integer> res)  {
		wordids = wordid;
		try {
			hiddenids = getallhiddenids(wordid, res);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resids = res;

		ai = new Double[wordids.size()];
		for (int i = 0; i < wordids.size(); i++)
			ai[i] = 1.0;
		ah = new Double[hiddenids.size()];
		for (int j = 0; j < hiddenids.size(); j++)
			ah[j] = 1.0;
		ao = new Double[resids.size()];
		for (int k = 0; k < resids.size(); k++)
			ao[k] = 1.0;

		wi = new Double[wordids.size()][hiddenids.size()];
		for (int i = 0; i < wordids.size(); i++)
			for (int k = 0; k < hiddenids.size(); k++)
				wi[i][k] = getstrength(wordids.get(i), hiddenids.get(k), 0);

		wo = new Double[hiddenids.size()][resids.size()];

		for (int k = 0; k < hiddenids.size(); k++)
			for (int j = 0; j < resids.size(); j++)
				wo[k][j] = getstrength(hiddenids.get(k), resids.get(j), 1);

	}

	public Double[] feedforward() {
		double sum;
		for (int i = 0; i < wordids.size(); i++)
			ai[i] = 1.0;

		for (int k = 0; k < hiddenids.size(); k++) {
			sum = 0.0;
			for (int i = 0; i < wordids.size(); i++)
				sum = sum + ai[i] * wi[i][k];
			ah[k] = Math.atan(sum);
		}
		for (int j = 0; j < resids.size(); j++) {
			sum = 0.0;
			for (int k = 0; k < hiddenids.size(); k++)
				sum = sum + ah[k] * wo[k][j];

			ao[j] = Math.atan(sum);
			System.out.println(String.valueOf(ao[j]));
		}

		return ao;
	}

	public double dtanh(double y) {
		return 1.0 - y * y;
	}

	public Double[] getresult(List<Integer> wordid, List<Integer> res)  {
		setupnetwork(wordid, res);
		return feedforward();
	}

	public void backPropagate(Double[] targets, double N) {
		Double[] output_deltas = new Double[resids.size()];
		double error, change;
		for (int j = 0; j < resids.size(); j++)
			output_deltas[j] = 0.0;

		for (int j = 0; j < resids.size(); j++) {
			error = targets[j] - ao[j];
			output_deltas[j] = dtanh(ao[j]) * error;
		}

		Double[] hidden_deltas = new Double[hiddenids.size()];
		for (int k = 0; k < hiddenids.size(); k++)
			hidden_deltas[k] = 0.0;

		for (int k = 0; k < hiddenids.size(); k++) {
			error = 0.0;
			for (int j = 0; j < resids.size(); j++)
				error = error + output_deltas[j] * wo[k][j];
			hidden_deltas[k] = dtanh(ah[k]) * error;
		}

		for (int k = 0; k < hiddenids.size(); k++)
			for (int j = 0; j < resids.size(); j++) {
				change = output_deltas[j] * ah[k];
				wo[k][j] = wo[k][j] + N * change;
				// System.out.println(String.valueOf(wo[k][j]));
			}

		for (int i = 0; i < wordids.size(); i++)
			for (int k = 0; k < hiddenids.size(); k++) {
				change = hidden_deltas[k] * ai[i];
				wi[i][k] = wi[i][k] + N * change;
				// System.out.println(String.valueOf(wi[i][k]));
			}
		return;

	}

	public void trainquery(List<Integer> wordid, List<Integer> res, List<Integer> selectres)  {
		generatehiddennode(wordid, res);
		setupnetwork(wordid, res);
		feedforward();
		Double[] targets = new Double[res.size()];
		for (int j = 0; j < res.size(); j++)
			targets[j] = 0.0;
		for (int j = 0; j < selectres.size(); j++)
			targets[res.indexOf(selectres.get(j))] = 1.0;
		backPropagate(targets, 0.5);
		updatedatabase();

	}

	public void updatedatabase() {
		for (int i = 0; i < wordids.size(); i++)
			for (int k = 0; k < hiddenids.size(); k++)
				setstrength(wordids.get(i), hiddenids.get(k), 0, wi[i][k]);

		for (int k = 0; k < hiddenids.size(); k++)
			for (int j = 0; j < resids.size(); j++)
				setstrength(hiddenids.get(k), resids.get(j), 1, wo[k][j]);

	}

	public static void main(String args[]) {
		
		net nn = new net();
		nn.connectDB();

		List<Integer> wordid = new ArrayList<Integer>();
		List<Integer> res = new ArrayList<Integer>();
		List<Integer> selectres = new ArrayList<Integer>();

		wordid.add(0, 101);// world
		// wordid.add(102);//river
		wordid.add(1, 103);// bank

		res.add(0, 201);// WorldBank
		res.add(1, 202);// river
		res.add(2, 203);// earth

		selectres.add(201);
		nn.makeTable();
		// nn.generatehiddennode(wordid, res);
		// nn.getresult(wordid, res);

		// for(int i = 0 ;i<30;i++)
		{
			nn.trainquery(wordid, res, selectres);
			nn.getresult(wordid, res);
		}

		nn.closeDB();

	}

}
