from PIL import Image, ImageDraw
import csv
import sys
import subprocess
import math

file = str(sys.argv[1])
renders = int(str(subprocess.run(['wc', file], stdout=subprocess.PIPE)).split()[4])

width = 0
height = 0

numLEDs = 240

try:
    if sys.argv[2]:
        columns = math.ceil(renders / 2000)
        width = columns * numLEDs + 10 * (columns - 1)
        height = 2000
except IndexError:
    width = numLEDs
    height = renders
out = Image.new("RGB", (width, height))
dout = ImageDraw.Draw(out)

# print(str(subprocess.run(['wc', 'colors.csv'], stdout=subprocess.PIPE)).split()[4])

with open(file, 'r') as f:
    reader = csv.reader(f)
    print('renders: {}'.format(renders))
    i = 0
    try:
        if sys.argv[2]:
            try:
                for row in reader:
                    for v in range(0, numLEDs - 1):
                        dout.point((v + ((numLEDs + 10) * math.floor(i / 2000)), i % 2000),
                                   fill=(int(row[3 * v]), int(row[3 * v + 1]), int(row[3 * v + 2])))
                    for v in range(0, 10):
                        dout.point((v + ((numLEDs + 10) * math.floor(i / 2000)) + numLEDs, i % 2000), fill=(32, 32, 32))
                    i += 1
                    if i % 1000 == 0:
                        print(i)
            except:
                i = 0
    except IndexError:
        try:
            for row in reader:
                for v in range(0, numLEDs - 1):
                    dout.point((v, i), fill=(int(row[3 * v]), int(row[3 * v + 1]), int(row[3 * v + 2])))
                i += 1
                if i % 1000 == 0:
                    print(i)
        except:
            i = 0
# print(row[0] + " " + row[1] + " " + row[2])
out.save("{}.png".format(file))
