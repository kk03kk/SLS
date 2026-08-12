import unittest


class SB3IntegrationTests(unittest.TestCase):
    def test_maskable_ppo_can_train_on_vectorized_simulator(self):
        try:
            from sb3_contrib import MaskablePPO
            from stable_baselines3.common.vec_env import DummyVecEnv
            from spirecomm.envs import SimulatorSTSEnv
        except (ImportError, RuntimeError) as exc:
            self.skipTest(str(exc))

        vector_env = DummyVecEnv([lambda: SimulatorSTSEnv() for _ in range(2)])
        try:
            model = MaskablePPO(
                "MultiInputPolicy",
                vector_env,
                n_steps=8,
                batch_size=8,
                n_epochs=1,
                policy_kwargs={"net_arch": [16]},
                seed=1,
                verbose=0,
            )
            model.learn(total_timesteps=16)
        finally:
            vector_env.close()


if __name__ == "__main__":
    unittest.main()
