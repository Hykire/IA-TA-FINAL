from os import chdir, system, open, devnull, O_WRONLY, dup2
from sys import exit
from subprocess import Popen, PIPE, STDOUT
from pynput import keyboard

chdir(r"../../../mariosolve")

return_code = system("javac -sourcepath ./src ./src/PlayLevel.java")
if return_code != 0:
	print("Ocurrio un error al tratar de compilar el proyecto")
	exit(return_code)

left = right = jump = down = run = b"f"
#p = Popen(['java', '-jar', '/home/ruggi/Documents/Pucp/IA/TA/ta_fin_fin/mariosolve/out/artifacts/mariosolve_jar/mariosolve.jar'], stdout=PIPE, stderr=STDOUT)
p = Popen(['java', '-classpath', './src', 'PlayLevel'], stdin=PIPE, stdout=PIPE)

def on_press(key):
    global left, right, jump, down ,run
    try:
        #print('alphanumeric key {0} pressed'.format(
        #    key.char))
        if key.char == ('a' or 'A'): left = b"t"
        if key.char == ('d' or 'D'): right = b"t"
        if key.char == ('w' or 'W'): jump = b"t"
        if key.char == ('s' or 'S'): down = b"t"
        return key.char
    except AttributeError:
        #print('special key {0} pressed'.format(
        #    key))
        if key == keyboard.Key.left: left = b"t"
        if key == keyboard.Key.right: right = b"t"
        if key == keyboard.Key.up: jump = b"t"
        if key == keyboard.Key.down: down = b"t"
        if key == keyboard.Key.space: run = b"t"
        return key

def on_release(key):
    global left, right, jump, down,run
    #print('{0} released'.format(
    #    key))
    try:
        if key.char == ('a' or 'A'): left = b"f"
        if key.char == ('d' or 'D'): right = b"f"
        if key.char == ('w' or 'W'): jump = b"f"
        if key.char == ('s' or 'S'): down = b"f"
    except:
        if key == keyboard.Key.left: left = b"f"
        if key == keyboard.Key.right: right = b"f"
        if key == keyboard.Key.up: jump = b"f"
        if key == keyboard.Key.down: down = b"f"
        if key == keyboard.Key.space: run = b"f"
    if key == keyboard.Key.esc:
        # Stop listener
        return False

listener = keyboard.Listener(
        on_press=on_press,
        on_release=on_release)

listener.start()

from time import sleep
while (True):
	try:
		movements = left+b","+right+b","+down+b","+run+b","+jump+b"\n"
		p.stdin.write(movements)
		p.stdin.flush()
		result = p.stdout.readline()
		print(result)
	except:
		devnull_ = open(devnull, O_WRONLY)
		dup2(devnull_, p.stdout.fileno())
		break
p.wait()

# p.stdin.write(b"a\n")
# p.stdin.flush()
# result = p.stdout.readline()
# print(result)



# p.stdin.write(b"a\n")
# p.stdin.flush()
# result = p.stdout.readline()
# print(result)





# p.stdin.write(b"b\n")
# p.stdin.flush()
# result = p.stdout.readline()
# print(result)

# p.stdin.write(b"bebecitad\n")
# p.stdin.flush()
# result = p.stdout.readline()
# print(result)

# p.stdin.write(b'asdd\n')
# p.stdin.flush()
# result = p.stdout.readline()
# print(result)

# p.stdin.close()


# Cuando java muera seremos felices , y saldra del wait
#p.wait()
# result = p.stdout.read()
# for line in result:
#   print ((str) line)