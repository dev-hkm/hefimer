interface Env {
  CRON_SECRET: string;
}

export default {
  async scheduled(_event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
    ctx.waitUntil(
      fetch(`https://hefimer.pages.dev/api/cron/cleanup?secret=${encodeURIComponent(env.CRON_SECRET)}`)
        .then(async (response) => {
          if (!response.ok) throw new Error(`Cleanup returned ${response.status}: ${await response.text()}`);
        }),
    );
  },
};
