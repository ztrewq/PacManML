#!/usr/bin/env python
# -*- coding: utf-8 -*-

import matplotlib.pyplot as plt
import csv

values = []

try:
    inputFile = open("values.txt", 'rt')
    valuesCsv = csv.reader(open('values.txt','rb'),delimiter=',')
    
    for row in valuesCsv:
        valuesString = row
    for word in valuesString:
        word.strip()
        values.append(int(word))

except IOError:
    print "Input value file not found."

plt.plot(values)
plt.ylabel('score')
plt.xlabel('trial no.')

plt.show()
