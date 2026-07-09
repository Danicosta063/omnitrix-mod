    private void processFrame(Bitmap frame) {
        if (frame == null) return;
        screenWidth = frame.getWidth();
        screenHeight = frame.getHeight();
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return;

            // Detecta inicio de luta: HP > 0 nos dois lados E movimento
            if (!inFight && state.p1Hp > 0 && state.oppHp > 0 && state.motion > 5) {
                inFight = true;
                pausedFrames = 0;
                Log.i(TAG, "Luta iniciada P1=" + state.p1Hp + " opp=" + state.oppHp);
            }

            // Detecta pausa: movimento quase zero por varios frames
            if (inFight && state.motion < 3) {
                pausedFrames++;
                if (pausedFrames > 5) {
                    // Jogo provavelmente pausado ou em transicao - parar de tocar
                    return;
                }
            } else {
                pausedFrames = 0;
            }

            if (inFight) {
                if (state.oppHp <= 0) { onFightEnd(true);  return; }
                if (state.p1Hp  <= 0) { onFightEnd(false); return; }
            }

            // Recompensa densa: dano causado - dano sofrido + bonus por acerto
            double reward = 0;
            if (lastState != null && lastActionIndex >= 0 && inFight) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (state.hitFlash) reward += 2.0;  // bonus por acertar golpe
                if (dmgDealt == 0 && dmgTaken == 0 && !state.hitFlash) reward = -0.1;
            }

            int stateIdx = brain.discretizeState(state);
            int actionIdx = brain.chooseAction(stateIdx);

            if (lastState != null && lastActionIndex >= 0) {
                int lastIdx = brain.discretizeState(lastState);
                brain.update(lastIdx, lastActionIndex, reward, stateIdx);
            }

            final int act = actionIdx;
            mainHandler.post(() -> executeAction(act));

            lastState = state;
            lastActionIndex = actionIdx;

        } catch (Exception e) {
            Log.e(TAG, "Erro em processFrame", e);
        } finally {
            frame.recycle();
        }
    }
