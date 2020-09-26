package com.edo.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Non rubare<br/><br/>
 * 
 * Come viengono compressi file con questo algoritmo:<br/>
 * <ol>
 * <li>conta le occorrenze di ogni carattere;</li>
 * <li>assegna ad ogni carattere una frequenza in modo da far occupare meno spazio ai caratteri più frequenti;</li>
 * <li>salva la sequenza finale in un file.</li>
 * </ol><br/>
 * I file presentano anche un header composto come segue:<br/>
 * --------------------Header---------------------<br/>
 *  P1 Numero di bit da leggere nella decodifica<br/>
 *  P2 carattere-sequenza (per ogni carattere)<br/>
 *  -----------------Fine Header------------------<br/>
 *  P1 e i vari P2 sono separati dal carattere ':', mentre l'header e il contenuto del file sono separati dal carattere '\0'.
 * 
 * @author Stucchi Edoardo
 * @version 1.0.0
 */
public class EntryPoint
{
	/**
	 * Questa class rappresenta il nodo di un albero binario, infatti ha a sua volta due nodi, la <b>destra</b> e la <b>sinistra</b>.
	 */
	public class Node
	{
		public char ch; 		   //carattere
		public int bit; 		   //occorrenza
		public Node left, right;   //figli
		
		public Node(char ch, int bit)
		{
			this.ch = ch;
			this.bit = bit;
			this.left = null;
			this.right = null;
		}
		
		public Node(Node n)
		{
			this.ch = n.ch;
			this.bit = n.bit;
			this.left = n.left;
			this.right = n.right;
		}
	}
	
	private static final boolean GEN_FILE = true;
	private HashMap<Character, Integer> mappedChars;
	private HashMap<Character, String> sequence;
	private File f = new File("src/files/file0.txt");
	private Node root;
	
	public EntryPoint()
	{
		if(GEN_FILE) //qua chiede se generare il file nel caso sia andato a farsi benedire per qualche motivo
			genFile();
		
		Scanner sc = new Scanner(System.in);
		String answer;
		
		System.out.println("Vuoi comprimere o decomprimere?");
		answer = sc.nextLine();
		sc.close();
		
		/**************** inizializza tutto ciò che c'è da inizializzare ***********************/
		// * variabili globali *
		mappedChars = new HashMap<Character, Integer>();
		sequence = new HashMap<Character, String>();
		root = new Node((char) -1, -1);
		// * variabili locali *
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		if(answer.equalsIgnoreCase("comprimere"))
		{
			System.out.println("Comprimere:");
			
			findOccurences(); //trova le occorrenze dei caratteri e le memorizza nell'hashmap "mappedChars"
			
			//stampa le occorrenze trovate nelle operazioni precedenti
			System.out.println("Occorrenze trovate:");
			for(Character c:mappedChars.keySet())
			{
				nodes.add(new Node(c, mappedChars.get(c)));
				System.out.println(c + ": " + mappedChars.get(c));
			}
			
			long fileSize = getFileSize(); //in base alle occorrenze intuisce quanto pesa il file d'origine
			System.out.println("Peso file: " + fileSize + "B");
			
			System.out.println("Informazioni: "); //calcola la probabilità del carattere (occorenze/dimTotFile) e l'autoinformazione (ceil(log2(1/probabilità)))
			for(Character c:mappedChars.keySet())
			{
				int info = (int) Math.ceil(log2(1 / ((double) mappedChars.get(c) / fileSize)));
				System.out.println(c + ": " + approxFirstDecimal(((double) mappedChars.get(c) / fileSize)) + ", autoinformazione: " + info + " bit");
			}
			
			int i, min = 0;
			
			//???
			while(nodes.size() > 2) //qua in tutto questo while crea l'albero binario
			{
				Node l, r;
				
				i = findMin(nodes); //trova la prima occorrenza più bassa
				l = new Node(nodes.get(i));
				min += nodes.get(i).bit;
				nodes.remove(i);
				
				i = findMin(nodes); //trova la seconda occorrenza più bassa
				r = new Node(nodes.get(i));
				min += nodes.get(i).bit;
				nodes.remove(i);
				
				Node newNode = new Node((char) -1, min); //qua fonde i due nodi con occorrenza minore in un unico nodo con occorrenza nodo0 + nodo1
				newNode.left = new Node(l);
				newNode.right = new Node(r);
				nodes.add(newNode);
				
				min = 0;
			}
			
			//lega gli ultimi due nodi rimasti al nodo radice
			root.right = nodes.get(0);
			if(nodes.size() > 1)
				root.left = nodes.get(1);
			nodes.clear();
			
			//una funzione ricorsiva che ha il compito di assegnare ad ogni carattere dell'albero binario una stringa rappresentante la sua codifica
			passThroughTree(root, "");
			
			//mostra le sequenze che sono state assegnate dall'algoritmo ad ogni carattere
			System.out.println("Sequenze:");
			for(char c:sequence.keySet())
				System.out.println(c + ": " + sequence.get(c));
			
			//fa un bel paragone di dimensione rispetto a prima della compressione del file (senza però scriverlo effettivamente)
			System.out.println("Peso originale: " + (getFileSize() * 8) + " bit (" + getFileSize() + "B)");
			System.out.println("Peso ridotto: " + (getReducedFileSize()) + " bit (" + (int) Math.ceil(getReducedFileSize() / 8) + "B)");
			System.out.println("Congratulazioni, hai risparmiato " + (getFileSize() - (int) Math.ceil(getReducedFileSize() / 8)) + "B!!");
			
			compress();
		}
		else
		{
			System.out.println("Decomprimere:");
			
			readCompressed();
		}
	}
	
	public static void main(String[] args) { new EntryPoint(); }
	
	private void readCompressed()
	{
		File file = new File("src/files/compressedFile0.edo");
		
		try
		{
			if(file.exists())
			{
				long readBit;
				String buffer = "";
				String translation = "";
				
				//header
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				int bytechar = reader.read();
				
				while((char) bytechar != '\0')
				{
					buffer += (char) bytechar;
					bytechar = reader.read();
				}
				
				String[] header = buffer.split("\1");
				readBit = Long.parseLong(header[0]);
				for(int i = 1; i < header.length; i++)
				{
					String[] sequence = header[i].split("\2");
					this.sequence.put(sequence[0].charAt(0), sequence[1]);
				}
				
				for(char c:this.sequence.keySet())
					System.out.println(c + ": " + this.sequence.get(c));
				
				buffer = "";
				bytechar = reader.read();
				while(bytechar != -1)
				{
					String local = Integer.toBinaryString(bytechar);
					int l = local.length();
					
					for(int i = 0; i < 8 - l; i++)
						local = "0" + local;
					
					buffer += local;
					bytechar = reader.read();
				}
				
				reader.close();
				
				//file
				String sequence = "";
				buffer = buffer.substring(0, (int) Math.min(readBit, buffer.length()));
								
				for(int i = 0; i < buffer.length(); i++)
				{
					sequence += buffer.charAt(i);
					
					for(char c:this.sequence.keySet())
					{
						if(sequence.equals(this.sequence.get(c)))
						{
							translation += c;
							sequence = "";
							break;
						}
					}
				}
				
				//decompression
				File output = new File("src/files/decompressedFile0.txt");
				
				try
				{
					if(output.createNewFile())
					{
						FileWriter fw = new FileWriter(output);
						fw.write(translation);
						fw.close();
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				System.out.println("File decompresso generato con successo.");
			}
			else
				System.err.println("Errore: il file non esiste");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void compress()
	{
		String buffer = "";
		File file = new File("src/files/compressedFile0.edo");
		
		try
		{
			if(file.createNewFile())
			{
				BufferedReader reader = new BufferedReader(new FileReader(f));
				int c = 0;
				
				while((c = reader.read()) != -1)
					buffer += (char) c;
				
				reader.close();
				
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
				
				//header
				fw.write(new Long(getReducedFileSize()).toString());
				fw.write('\1');
				for(char ch:sequence.keySet())
					fw.write(ch + "\2" + sequence.get(ch) + '\1');
				fw.write('\0');
				
				//file
				String byteBuffer = "";
				for(int i = 0; i < buffer.length(); i++)
				{
					if(sequence.get(buffer.charAt(i)) != null)
					{
						for(int j = 0; j < sequence.get(buffer.charAt(i)).length(); j++)
						{
							byteBuffer += sequence.get(buffer.charAt(i)).charAt(j);
							
							if(byteBuffer.length() == 8)
							{
								fw.write(Integer.parseInt(byteBuffer, 2));
								byteBuffer = "";
							}
						}
					}
				}
				
				if(byteBuffer.length() > 0)
				{
					while(byteBuffer.length() != 8)
						byteBuffer += "0";
					fw.write(Integer.parseInt(byteBuffer, 2));
				}
				
				fw.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private long getReducedFileSize()
	{
		long size = 0;
		
		for(char c:mappedChars.keySet())
			size += mappedChars.get(c) * sequence.get(c).length();
		
		return size;
	}
	
	private void passThroughTree(Node node, String s)
	{
		if(node.left == null && node.right == null)
			sequence.put(node.ch, s);
		
		if(node.left != null)
			passThroughTree(node.left, s + "0");
		if(node.right != null)
			passThroughTree(node.right, s + "1");
	}
	
	private int findMin(ArrayList<Node> list)
	{
		if(list.size() > 0)
		{
			int min = list.get(0).bit, index = 0;
			
			for(int i = 0; i < list.size(); i++)
			{
				if(list.get(i).bit < min)
				{
					min = list.get(i).bit;
					index = i;
				}
			}	
			
			return index;
		}
		
		return -1;
	}
	
	private double log2(double n) { return Math.log(n) / Math.log(2); }
	
	private double approxFirstDecimal(double n)
	{
		int cont = 0;
		double m = n;
		
		while(Math.floor(m) == 0)
		{
			m *= 10;
			cont++;
		}
		
		if(cont == 0)
			return m;
		
		return (double) Math.round(m) / Math.pow(10, cont);
	}
	
	private long getFileSize()
	{
		long size = 0;
		
		for(Character c:mappedChars.keySet())
			size += mappedChars.get(c);
		
		return size;
	}
	
	private void findOccurences()
	{
		FileReader fr;
		try
		{
			fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			int c = 0;             
			while((c = br.read()) != -1)
			{
				Character read = new Character((char) c);
				mappedChars.put(read, mappedChars.containsKey(read) ? mappedChars.get(new Character((char) c)) + 1 : 1);
			}
			
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void genFile()
	{
		try
		{
			if(f.createNewFile())
			{				
				FileWriter fw = new FileWriter(f);
				
				for(int i = 0; i < 1000; i++)
				{
					fw.write('b');
					
					if(i < 250)
						fw.write('a');
					if(i < 125)
						fw.write('c');
					if(i < 10)
						fw.write('d');
				}
				
				fw.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
