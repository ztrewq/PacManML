package pacman.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class ParaValueList implements Serializable{

	private static final long serialVersionUID = 1L;
	private ArrayList<ParaValuePair> list = new ArrayList<ParaValuePair>();
	
	public void add(ParaValuePair pair){
		list.add(pair);
	}
	
	//prints out all parameter values line for line (each line is a individual set of parameters)
	public void print(){
		for(int i = 0; i < list.size(); i++){
			System.out.print("points: "+list.get(i).getValue()+"; para: ");
			println(list.get(i).getParameter());
		}
	}

	private void println(Vector fs) {
		for(int i = 0; i < fs.getDimension(); i++){
			System.out.print(fs.getAt(i) + "; ");
		}
		System.out.println();
	}
	
	public void printLast(){
		System.out.print("points: "+list.get(list.size()-1).getValue()+"; para: ");
		println(list.get(list.size()-1).getParameter());
	}
	
	//Loads savelist from file
		public static ParaValueList createFromFile(String file){
			
			try{
				FileInputStream fout = new FileInputStream(file+".sav");
				ObjectInputStream out = new ObjectInputStream(fout);
				ParaValueList ret = (ParaValueList)out.readObject();
				out.close();
				fout.close();
				return ret;
			}catch(Exception e){
				System.out.println("CANT CREATE MYCONTROLLER FROM FILE: "+file+".sav :"+e);
			}
			return null;
		}
	
	//Writes list to file
	public static void writeToFile(ParaValueList sl, String file){
		try{
			FileOutputStream fout = new FileOutputStream(file+".sav");
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(sl);
			out.close();
			fout.close();
		}catch(Exception e){
			System.out.println("SAVE ERROR WRITING TO FILE: "+file+".sav :"+e);
		}
	}
}

