package pacman.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import pacman.controllers.MyController;

public class Savelist implements Serializable{
	
	private ArrayList<float[]> list = new ArrayList<float[]>();

	public void add(float[] val){
		list.add(val);
	}
	
	//prints out all parameter values line for line (each line is a individual set of parameters)
	public void print(){
		for(int i = 0; i < list.size(); i++){
			println(list.get(i));
		}
	}

	private void println(float[] fs) {
		for(int i = 0; i < fs.length; i++){
			System.out.print(fs[i]+"; ");
		}
		System.out.println();
	}
	
	public void printLast(){
		println(list.get(list.size()-1));
	}
	
	//Loads savelist from file
		public static Savelist createFromFile(String file){
			
			try{
				FileInputStream fout = new FileInputStream(file+".sav");
				ObjectInputStream out = new ObjectInputStream(fout);
				Savelist ret = (Savelist)out.readObject();
				out.close();
				fout.close();
				return ret;
			}catch(Exception e){
				System.out.println("CANT CREATE MYCONTROLLER FROM FILE: "+file+".sav :"+e);
			}
			return null;
		}
	
	//Writes list to file
	public static void writeToFile(Savelist sl, String file){
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
