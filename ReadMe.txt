1-	Install Ptolemy II,
2-	Go to the directory of your installation, then ptolemy/domains,
3-	Replace the atc folder with our atc folder,
4-	Note that we have changed several parameters of the DE director and the SDF director from private/protected to public. 
	Therefore, replace the DEDirector.java and SDFDirector.java with the previous ones,
5-	Invoke the vergil,
6-	After coming vergil up, go to the atc/demo/FirstPolicy and select a model. 


To run a Magnifier model:

	open MModel15_15.xml for a 15*15 mesh structure
	OR
	open model18_18.xml for a 18*18 mesh structure
	
To run a Non-Magnifier model (Non-compositional model):
	
	open NModel15_15.xml for a 15*15 mesh structure
	OR
	open Nmodel18_18.xml for a 18*18 mesh structure


An input for a model with 15*15 mesh structure is placed in the input-15-15 folder.
An input for a model with 18*18 mesh structure is placed in the input-18-18 folder.

Place an input in the ptII directory and run the model. The number of states are written in outputS1.txt.
To execute the model for the second time, remove outputS1.txt if created any.

Note that we executed the code using an Ubuntu machine. To calculate the execution time in Ubuntu we used "time" shell command.

Do not hesitate to contact maryam.bagheri1989@gmail.com if you have any problem. You can download the .class files from http://www.ce.sharif.ir/~mbagheri/MagImp.zip.

Good Luck,
Maryam Bagheri.




