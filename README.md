## Local Development

This repo was created at [this link](https://app.netlify.com/start/deploy?repository=https://github.com/netlify-templates/one-click-hugo-cms&stack=cms). Because the website has already been created, you won't need to do that to get started.

You'll need to set up [Docker](https://www.docker.com/) and [Netlify CI & Dev](https://docs.netlify.com/cli/get-started/#installation) for your machine.
Once you have Docker up and running:
1. `docker pull netlify/build:xenial` (this is not a speedy process)
2. `npm install netlify-cli -g`
3. `netlify login`
	3.1 This will open in a browser window. Click the authorize button.
4. `netlify init`

Once that's set up, run the server with `netlify dev`.
For more about how to use the Netlify CLI, use the commands `netlify` and `netlify help`.

## Deploying to Netlify

The Netlify site can be found [here](https://app.netlify.com/sites/stupefied-goldberg-35c5b7/overview).
Pushing to this repo will automatically start a deploy there.
The resulting site in production is [here](https://value.hexawise.com).
