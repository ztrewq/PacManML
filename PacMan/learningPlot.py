#!/usr/bin/env python
# -*- coding: utf-8 -*-

import matplotlib.pyplot as plt
import csv
import sys, getopt

def main(argv):
    values = []
    
    inputFile = ''
    try:
        opts, args = getopt.getopt(argv,  "hi:", ["ifile="])
    except getopt.GetoptError:
        print 'learningPlot.py -i <inputfile>'
        sys.exit(2)
    for opt, arg in opts:
      if opt == '-h':
        sys.exit()
      if opt in ("-i", "--ifile"):
         inputFile = arg
         print inputFile
         try:
             valuesCsv = csv.reader(open(inputFile,'rb'),delimiter=',')
             for row in valuesCsv:
                 valuesString = row
             for i in range(0, len(valuesString)-1):
                 valuesString[i].strip()
                 values.append(float(valuesString[i]))
         
         except IOError:
             print "Input value file not found."
         
         plt.plot(values)
         plt.ylabel('score')
         plt.xlabel('trial no.')
         
         plt.show()

if __name__ == "__main__":
    main(sys.argv[1:])
