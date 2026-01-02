# 🎮 Creep Colony - Expand and Conquer

A single-player strategy game inspired by StarCraft 1's Creep Colonies. Start as a lone colony and spread your creep across the map to achieve victory!

## 🎯 Game Concept

You control a Zerg-inspired creep colony with one mission: **EXPAND**. Spread your creep across the map by strategically placing new colonies and watch as your purple biomass consumes the terrain.

## 🕹️ How to Play

- **Start**: You begin with a single Creep Colony in the center of the map
- **Creep Spread**: Purple creep automatically spreads from your colonies
- **Energy**: Creep tiles generate energy over time
- **Expand**: Click on any creep tile to place a new colony (costs 50 energy)
- **Strategy**: More colonies = faster spread and more energy generation
- **Victory**: Cover 60% of the map with creep to win!

## 🚀 Running the Game

### Prerequisites
- Node.js (v14 or higher)
- Java (for Clojure/ClojureScript)

### Setup
```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The game will be available at `http://localhost:8080`

### Build for Production
```bash
npm run build
```

### Deploy to GitHub Pages
```bash
npm run deploy
```

The game is automatically deployed to GitHub Pages via GitHub Actions when you push to the branch. The live version will be available at:
- `https://<username>.github.io/clj-creep/`

## 🎮 Game Mechanics

- **Creep Spreading**: Creep spreads to adjacent tiles with a chance-based system
- **Energy System**: Each creep tile generates 0.1 energy per tick
- **Colony Placement**: Colonies can only be placed on existing creep
- **Colony Cost**: 50 energy per colony
- **Win Condition**: Achieve 60% map coverage

## 🛠️ Technical Stack

- **ClojureScript**: Game logic and state management
- **Reagent**: Reactive UI framework
- **Shadow-CLJS**: Build tool and development server
- **HTML5/CSS**: Rendering and styling

## 🎨 Game Features

- ✅ Automatic creep spreading algorithm
- ✅ Resource management (energy system)
- ✅ Strategic colony placement
- ✅ Real-time statistics
- ✅ Victory condition
- ✅ Pause/Resume functionality
- ✅ Visual feedback with animations
- ✅ Responsive grid-based map

## 📝 Development

The game is built using functional reactive programming principles:
- Game state is managed with Reagent atoms
- Pure functions handle game logic
- Game loop runs at regular intervals
- UI automatically updates on state changes

### Project Structure
```
clj-creep/
├── src/
│   └── creep_colony/
│       └── core.cljs          # Main game logic and UI
├── public/
│   └── index.html             # HTML and styles
├── shadow-cljs.edn            # Build configuration
├── deps.edn                   # Clojure dependencies
└── package.json               # NPM dependencies
```

## 🎯 Strategy Tips

1. **Early Expansion**: Place new colonies as soon as you can afford them
2. **Spread Wide**: Colonies on the edges spread to new territory faster
3. **Resource Management**: Don't spend all your energy - keep some reserve
4. **Grid Coverage**: Focus on spreading horizontally and vertically for efficient coverage

## 🔮 Future Ideas

- Enemy colonies that compete for space
- Different colony types (fast spread, high energy, defensive)
- Special abilities and upgrades
- Multiple difficulty levels
- Obstacles and terrain types
- Sound effects and music

## 📜 License

MIT License - Feel free to modify and expand the game!

---

**Made with ❤️ and ClojureScript**

*For the Swarm!* 🐛
