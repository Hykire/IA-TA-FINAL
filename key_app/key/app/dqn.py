import random
import numpy as np
from os import open as open_
from os import chdir,system, devnull, O_WRONLY, dup2
from sys import exit
from subprocess import Popen, PIPE, STDOUT
from pynput import keyboard
import pickle
from collections import deque
from keras.models import Sequential, load_model
from keras.layers import Dense
from keras.optimizers import Adam
from keras.callbacks import ModelCheckpoint

random.seed(4)

reward_stuck = reward_forward = reward_kill_enemy = timer_without_killing_enemies = 0
history = []

def form_action(left, right, down, run, jump):
  return left+b","+right+b","+down+b","+run+b","+jump+b"\n"

def generate_reward(state):
  global reward_stuck, reward_forward, reward_kill_enemy, timer_without_killing_enemies
  
  max_reward_stuck = -100
  max_reward_forward = 100
  max_reward_kill_enemy = 40
  max_timer_without_killing_enemies = 10
  
  collision = stuck = forward = backward = False
  
  # Si un enemigo le inflingio daño al agente
  if state[5] == '1': collision = True
  
  # Si esta atascado
  if state[8] == '1': stuck = True
  
  d1 = state[9]
  d2 = state[10]
  d3 = state[11]
  d4 = state[12]
  d5 = state[13]
  d6 = state[14]
  d7 = state[15]
  d8 = state[16]
  # Si se mueve hacia adelante
  if (d1 or d2 or d3 or d8) == '1': backward = True
  # Si se mueve hacia adelante
  if (d4 or d5 or d6 or d7) == '1': forward = True
  
  # Si un enemigo le inflingio daño al agente
  kill_enemy = True if state[23] == '1' else False
  
  
  # Elaboracion del reward
  reward_collision = -30 if collision and state[1] == '0' else 0
  if stuck and reward_stuck > max_reward_stuck: reward_stuck -= 10
  if not stuck: reward_stuck = 0
  if forward and reward_forward < max_reward_forward: reward_forward += 10
  if not forward: reward_forward = 0
  reward_backward = -30 if backward else 0
  if kill_enemy:
    if reward_forward == max_reward_forward: reward_forward = 10 if forward else 0
    if reward_kill_enemy < max_reward_kill_enemy: reward_kill_enemy += 20
  if not kill_enemy: timer_without_killing_enemies += 1
  if timer_without_killing_enemies == max_timer_without_killing_enemies: timer_without_killing_enemies = reward_kill_enemy = 0
  
  
  # El reward esta entre -160 y 140
  reward = (reward_collision + reward_stuck + reward_forward + reward_backward + reward_kill_enemy)/100
#   if reward <= -100: reward = -1
#   elif reward >= 100: reward = 1
#   else: reward /= 100
  
  # Se devuelve a 0 el valor de reward_kill_enemy luego de agregarlo en reward si ya alcanzo su maximo
  if reward_kill_enemy == max_reward_kill_enemy: reward_kill_enemy = 0
  return reward

def lookahead(iterable):
    """Pass through all values from the given iterable, augmented by the
    information if there are more values to come after the current one
    (True), or if it is the last value (False).
    """
    # Get an iterator and pull the first value.
    it = iter(iterable)
    last = next(it)
    # Run the iterator to exhaustion (starting from the second value).
    for val in it:
        # Report the *previous* value (more to come).
        yield last, False
        last = val
    # Report the last value.
    yield last, True

class DQNAgent:
  def __init__(self, state_size, action_size, load=False, filepath=""):
    self.state_size = state_size     # tamaño de un estado (numero de atributos que representan un estado)
    self.action_size = action_size   # tamaño del vector de acciones 
    self.memory = deque(maxlen=2000)  # define la memoria del agente (2000 registros como maximo)
    self.gamma = 0.6                 # discount rate
    self.learning_rate = 0.001        # taza de aprendizaje 

    self.epsilon = 1.0          # factor de exploration inicial
    self.epsilon_min = 0.01     # factor de exploration minimo
    self.epsilon_decay = 0.995  # factor de decaimiento del factor de exploracion
    if load:
      self.load(filepath)
    else:
      self.model = self._build_model()  # construye el modelo neuronal para estimar las utilidades

  def _build_model(self):
    model = Sequential()   # Informa que las capas que se van agregar son secuenciales
    model.add(Dense(30, input_dim=self.state_size, activation='relu'))
    model.add(Dense(50, activation='relu'))
    model.add(Dense(25, activation='relu'))
    model.add(Dense(self.action_size, activation='linear'))
    model.compile(loss='mse', optimizer = Adam(lr=self.learning_rate)) # la funcion de perdida es el error cuadratico medio (mse)
    return model

  # con cada paso del entorno se almacena una quintupleta (experiencia simple): (estado, accion, reward resultante, nuevo estado, done)
  # done es un flag que indica que el entorno cayo en un estado terminal
  def remember(self, state, action, reward, next_state, done): 
    self.memory.append((state, action, reward, next_state, done))

  # retorna una accion.  
  def get_action(self, state):
    if np.random.rand() <= self.epsilon:  # retorna una accion aleatoria con probabilidad self.epsilon
        return random.randrange(self.action_size)
    action_values = self.model.predict(state) # obtiene los q-valores predichos por el modelo para cada accion
    return np.argmax(action_values[0])  # retorna la accion con el maximo q-valor predicho

  def train(self, batch_size, episode_to_perform_train): # ajusta la red neuronal con una muestra de su memoria de tamaño batch_size
    global history
    # define the checkpoint
    #filepath = "weights.{epoch:02d}-{val_loss:.2f}.hdf5"
    filepath = "../key_app/key/app/checkpoints/model.episode_to_perform_train:{:04d}".format(episode_to_perform_train)
    filepath += "-{loss:.2f}.h5"
    checkpoint = ModelCheckpoint(filepath, monitor='loss', verbose=1, save_best_only=True, mode='min')
    callbacks_list = [checkpoint]
    
    minibatch = random.sample(self.memory, batch_size) # obtiene una muestra de la memoria de experiencias simples
    for (state, action, reward, next_state, done), last_iteration in lookahead(minibatch):  # por cada experiencia simple en el minibatch
      target = reward
      if not done:  # si no es estado terminal -> el target es la suma descontada de rewards futuros
        target = (reward + self.gamma * np.amax(self.model.predict(next_state)[0]))

      target_f = self.model.predict(state)  # por defecto, los valores del vector target son los valores Q del estado predichos por el modelo
      target_f[0][action] = target   # solo modifica un valor Q del vector target, aquel relativo a la accion ejecutada en la experiencia simple (alli coloca el valor future discounted reward) 
      if last_iteration:
        history.append(self.model.fit(state, target_f, epochs=1, verbose=0, callbacks=callbacks_list)) # ajusta pesos de la red (usa los ultimos pesos como pesos iniciales) y se guarda el modelo
      else:
        history.append(self.model.fit(state, target_f, epochs=1, verbose=0)) # ajusta pesos de la red (usa los ultimos pesos como pesos iniciales)
    
    if self.epsilon > self.epsilon_min: # si no esta en el valor minimo del factor de exploracion -> hace un decaimiento del factor de exploracion
      epsilon_aux = self.epsilon * self.epsilon_decay
      self.epsilon = self.epsilon_min if epsilon_aux < self.epsilon_min else epsilon_aux

  def load(self, filepath):
    self.model = load_model(filepath)

  def save(self, name):
    self.model.save_weights(name)

  def obtain_movements(self, action):
    left = right = jump = down = run = b'f'

    if action == 0:
      left = b't'
    if action == 1:
      right = b't'
    if action == 2:
      jump = b't'
    if action == 3:
      down = b't'
    if action == 4:
      run = b't'
    movements = form_action(left, right, down, run, jump)

    return movements

chdir(r"../../../mariosolve")

return_code = system("javac -sourcepath ./src ./src/PlayLevel.java")
if return_code != 0:
	print("Ocurrio un error al tratar de compilar el proyecto")
	exit(return_code)

use_keyboard = False
def on_press(key):
    global left, right, jump, down ,run, use_keyboard
    try:
        if use_keyboard:
          #print('alphanumeric key {0} pressed'.format(
          #    key.char))
          if key.char == ('a' or 'A'): left = b"t"
          if key.char == ('d' or 'D'): right = b"t"
          if key.char == ('w' or 'W'): jump = b"t"
          if key.char == ('s' or 'S'): down = b"t"
        if key.char == ('j' or 'J'): use_keyboard = True
        if key.char == ('k' or 'K'): use_keyboard = False
        return key.char
    except AttributeError:
        if use_keyboard:
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
        if use_keyboard:
          if key.char == ('a' or 'A'): left = b"f"
          if key.char == ('d' or 'D'): right = b"f"
          if key.char == ('w' or 'W'): jump = b"f"
          if key.char == ('s' or 'S'): down = b"f"
    except:
        if use_keyboard:
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


batch_size = 32    # tamaño del batch con el que se re-entrena el modelo neuronal
EPISODES = 3000     # numero de episodios

state_size = 24
action_size = 5

load_from_model = True
if load_from_model:
  # define the filepath
  
  filepath = "../key_app/key/app/checkpoints/model.episode_to_perform_train:0848-0.00.h5"
  agent = DQNAgent(state_size, action_size, True, filepath)  # instancia el agente deep q-learning
  # range_start = int(filepath.split("_")[-1].split("-")[0])
  rango = range(848 + 1, EPISODES)
else:
  agent = DQNAgent(state_size, action_size, False)  # instancia el agente deep q-learning
  rango = range(EPISODES)

for e in rango:   # por cada episodio
  done = False

  reward_stuck = reward_forward = reward_kill_enemy = timer_without_killing_enemies = 0
  
  left = right = jump = down = run = b'f' # se inicializan las acciones

  p = Popen(['java', '-classpath', './src', 'PlayLevel'], stdin=PIPE, stdout=PIPE) # se ejecuta la comunicacion con java
  
  number_of_steps = 0
  
  movements = form_action(left, right, down, run, jump) # iniciar el primer movimiento sin hacer nada
  p.stdin.write(movements)
  p.stdin.flush()
  
  state = (p.stdout.readline().decode("utf-8")[:-1]).split(",") # obtener el estado inicial del agente
  state = np.reshape(state, [1, state_size])
  
  while True:
    action = agent.get_action(state)
    if use_keyboard:
      movements = form_action(left, right, down, run, jump) # obtiene el movimiento a partir del teclado
    else:
      movements = agent.obtain_movements(action)   # obtiene el movimiento que realizara el agente
    p.stdin.write(movements)
    p.stdin.flush()
    next_state = (p.stdout.readline().decode("utf-8")[:-1]).split(",") # se obtiene el siguiente estado
    reward = generate_reward(next_state) # se genera el reward para el estado en el que se encuentra el agente
    if next_state[1] == '1': done = True # si pierde significa que termino el juego
    reward = reward if not done else -10  # si es estado terminal (muerte) el reward es -10
    next_state = np.reshape(next_state, [1, state_size])
    agent.remember(state.astype(np.int), action, reward, next_state, done) # almacena esta simple experiencia en la memoria del agente    
    state = next_state   # actualiza el estado actual al nuevo estado
    number_of_steps += 1
    
    if next_state[0][0] == '1' or done:  # si gana o pierde, imprime resultados del trial. El score del trial es el numero de pasos que logro ejecutar el agente
      print("episode: {}/{}, score: {}, e: {:.2}".format(e, EPISODES, number_of_steps, agent.epsilon))
      
      devnull_ = open_(devnull, O_WRONLY)
      dup2(devnull_, p.stdout.fileno())
      break
  if len(agent.memory) >= batch_size:  # si el agente tiene suficiente experiencias en su memoria -> ajusta su modelo neuronal 
    agent.train(batch_size, e)

with open('../key_app/key/app/history/history.pkl', 'wb') as f:
  pickle.dump(history, f)